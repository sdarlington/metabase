import React from "react";
import { IndexRoute, Route } from "react-router";
import TimelineIndexModal from "./containers/TimelineIndexModal";
import TimelineRouteModal from "./containers/TimelineRouteModal";

const getRoutes = () => {
  return (
    <Route path="timelines" component={TimelineRouteModal}>
      <IndexRoute component={TimelineIndexModal} />
    </Route>
  );
};

export default getRoutes;
