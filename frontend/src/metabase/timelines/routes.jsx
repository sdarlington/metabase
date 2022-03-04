import React from "react";
import { IndexRoute, Route } from "react-router";
import TimelineRouteModal from "./components/TimelineRouteModal";
import TimelineIndexModal from "./containers/TimelineIndexModal";

const getRoutes = () => {
  return (
    <Route path="timelines" component={TimelineRouteModal}>
      <IndexRoute component={TimelineIndexModal} />
    </Route>
  );
};

export default getRoutes;
