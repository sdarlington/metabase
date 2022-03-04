(ns metabase.driver.ignite
    (:require [clojure.java.jdbc :as jdbc]
              [clojure.set :as set]
              [clojure.tools.logging :as log]
              [honeysql.core :as hsql]
              [honeysql.format :as hformat]
              [metabase.driver :as driver]
              [metabase.driver.common :as driver.common]
              [metabase.driver.sql-jdbc.common :as sql-jdbc.common]
              [metabase.driver.sql-jdbc.connection :as sql-jdbc.conn]
              [metabase.driver.sql-jdbc.execute :as sql-jdbc.execute]
              [metabase.driver.sql-jdbc.execute.legacy-impl :as legacy]
              [metabase.driver.sql-jdbc.sync :as sql-jdbc.sync]
              [metabase.driver.sql.query-processor :as sql.qp]
              [metabase.driver.sql.query-processor.empty-string-is-null :as sql.qp.empty-string-is-null]
              [metabase.util.date-2 :as u.date]
              [metabase.util.honeysql-extensions :as hx]
              [metabase.util.i18n :refer [trs]])
    (:import [java.sql ResultSet Types]))

(driver/register! :ignite, :parent #{:sql-jdbc })

(defmethod driver/supports? [:ignite ] [_ _] false)

(defmethod driver/db-start-of-week :ignite
           [_]
           :monday)

(defmethod sql-jdbc.sync/database-type->base-type :ignite
           [_ database-type]
           ({:Boolean                   :type/Boolean
             :Integer                   :type/Integer
             :Bigint                    :type/BigInteger
             :Varbinary                 :type/*
             :Binary                    :type/*
             :Char                      :type/Text
             :Varchar                   :type/Text
             :Money                     :type/Decimal
             :Numeric                   :type/Decimal
             :Double                    :type/Decimal
             :Float                     :type/Float
             :Date                      :type/Date
             :Time                      :type/Time
             :TimeTz                    :type/TimeWithLocalTZ
             :Timestamp                 :type/DateTime
             :TimestampTz               :type/DateTimeWithLocalTZ
             :AUTO_INCREMENT            :type/Integer
             (keyword "Long Varchar")   :type/Text
             (keyword "Long Varbinary") :type/*} database-type))

(defmethod sql-jdbc.conn/connection-details->spec :ignite
           [_ {:keys [host port db dbname]
               :or   {host "localhost", port 10800, db ""}
               :as   details}]
           (-> (merge {:classname   "org.apache.ignite.IgniteJdbcThinDriver"
                       :subprotocol "ignite:thin"
                       :subname     (str "//" host ":" port "/" (or dbname db))}
                      (dissoc details :host :port :dbname :db :ssl))
               (sql-jdbc.common/handle-additional-options details)))

(defmethod sql.qp/unix-timestamp->honeysql [:ignite :seconds]
           [_ _ expr]
           (hsql/call :to_timestamp expr))

;; TODO - not sure if needed or not
(defn- cast-timestamp
  "ignite requires stringified timestamps (what Date/DateTime/Timestamps are converted to) to be cast as timestamps
  before date operations can be performed. This function will add that cast if it is a timestamp, otherwise this is a
  no-op."
  [expr]
  (if (instance? java.time.temporal.Temporal expr)
    (hx/cast :timestamp expr)
    expr))

(defn- date-trunc [unit expr] (hsql/call :date_trunc (hx/literal unit) (cast-timestamp expr)))
(defn- extract    [unit expr] (hsql/call :extract    unit              expr))

(def ^:private extract-integer (comp hx/->integer extract))

(defmethod sql.qp/date [:ignite :default]         [_ _ expr] expr)
(defmethod sql.qp/date [:ignite :minute]          [_ _ expr] (date-trunc :minute expr))
(defmethod sql.qp/date [:ignite :minute-of-hour]  [_ _ expr] (extract-integer :minute expr))
(defmethod sql.qp/date [:ignite :hour]            [_ _ expr] (date-trunc :hour expr))
(defmethod sql.qp/date [:ignite :hour-of-day]     [_ _ expr] (extract-integer :hour expr))
(defmethod sql.qp/date [:ignite :day]             [_ _ expr] (hx/->date expr))
(defmethod sql.qp/date [:ignite :day-of-month]    [_ _ expr] (extract-integer :day expr))
(defmethod sql.qp/date [:ignite :day-of-year]     [_ _ expr] (extract-integer :doy expr))
(defmethod sql.qp/date [:ignite :month]           [_ _ expr] (date-trunc :month expr))
(defmethod sql.qp/date [:ignite :month-of-year]   [_ _ expr] (extract-integer :month expr))
(defmethod sql.qp/date [:ignite :quarter]         [_ _ expr] (date-trunc :quarter expr))
(defmethod sql.qp/date [:ignite :quarter-of-year] [_ _ expr] (extract-integer :quarter expr))
(defmethod sql.qp/date [:ignite :year]            [_ _ expr] (date-trunc :year expr))

(defmethod sql.qp/date [:ignite :week]
           [_ _ expr]
           (sql.qp/adjust-start-of-week :ignite (partial date-trunc :week) (cast-timestamp expr)))

(defmethod sql.qp/date [:ignite :day-of-week]
           [_ _ expr]
           (sql.qp/adjust-day-of-week :ignite (hsql/call :dayofweek_iso expr)))

(defmethod sql.qp/->honeysql [:ignite :concat]
           [driver [_ & args]]
           (->> args
                (map (partial sql.qp/->honeysql driver))
                (reduce (partial hsql/call :concat))))

(defmethod sql.qp/->honeysql [:ignite :regex-match-first]
           [driver [_ arg pattern]]
           (hsql/call :regexp_substr (sql.qp/->honeysql driver arg) (sql.qp/->honeysql driver pattern)))

(defmethod sql.qp/->honeysql [:ignite :percentile]
           [driver [_ arg p]]
           (hsql/raw (format "APPROXIMATE_PERCENTILE(%s USING PARAMETERS percentile=%s)"
                             (hformat/to-sql (sql.qp/->honeysql driver arg))
                             (hformat/to-sql (sql.qp/->honeysql driver p)))))

(defmethod sql.qp/->honeysql [:ignite :median]
           [driver [_ arg]]
           (hsql/call :approximate_median (sql.qp/->honeysql driver arg)))

(defmethod sql.qp/add-interval-honeysql-form :ignite
           [_ hsql-form amount unit]
           (hx/+ (hx/->timestamp hsql-form) (hsql/raw (format "(INTERVAL '%d %s')" (int amount) (name unit)))))

(defn- materialized-views
  "Fetch the Materialized Views for a ignite `database`.
   These are returned as a set of maps, the same format as `:tables` returned by `describe-database`."
  [database]
  (try (set (jdbc/query (sql-jdbc.conn/db->pooled-connection-spec database)
                        ["SELECT SCHEMA_NAME AS \"schema\", TABLE_NAME AS \"name\" FROM SYS.TABLES;"]))
    (catch Throwable e
      (log/error e (trs "Failed to fetch materialized views for this database")))))

(defmethod driver/describe-database :ignite
           [driver database]
           (-> ((get-method driver/describe-database :sql-jdbc) driver database)
               (update :tables set/union (materialized-views database))))

(defmethod driver.common/current-db-time-date-formatters :ignite
           [_]
           (driver.common/create-db-time-formatters "yyyy-MM-dd HH:mm:ss z"))

(defmethod driver.common/current-db-time-native-query :ignite
           [_]
           "select to_char(CURRENT_TIMESTAMP, 'YYYY-MM-DD HH24:MI:SS TZ')")

(defmethod driver/current-db-time :ignite
           [& args]
           (apply driver.common/current-db-time args))

(defmethod sql-jdbc.execute/set-timezone-sql :ignite [_] "SET TIME ZONE TO %s;")

(defmethod sql-jdbc.execute/read-column [:ignite Types/TIME]
           [_ _ ^ResultSet rs _ ^Integer i]
           (when-let [s (.getString rs i)]
             (let [t (u.date/parse s)]
               (log/tracef "(.getString rs %d) [TIME] -> %s -> %s" i s t)
               t)))

(defmethod sql-jdbc.execute/read-column [:ignite Types/TIME_WITH_TIMEZONE]
           [_ _ ^ResultSet rs _ ^Integer i]
           (when-let [s (.getString rs i)]
             (let [t (u.date/parse s)]
               (log/tracef "(.getString rs %d) [TIME_WITH_TIMEZONE] -> %s -> %s" i s t)
               t)))
