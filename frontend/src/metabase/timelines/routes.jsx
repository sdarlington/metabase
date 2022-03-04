import React from "react";
import { IndexRoute, Route } from "react-router";
import TimelineIndexModal from "./containers/TimelineIndexModal";
import TimelineRouteModal from "./containers/TimelineRouteModal";
import NewTimelineModal from "./containers/NewTimelineModal";
import TimelineDetailsModal from "./containers/TimelineDetailsModal";
import EditTimelineModal from "./containers/EditTimelineModal";
import TimelineArchiveModal from "./containers/TimelineArchiveModal";
import NewEventWithTimelineModal from "./containers/NewEventWithTimelineModal";
import NewEventModal from "./containers/NewEventModal";
import EditEventModal from "./containers/EditEventModal";
import DeleteEventModal from "./containers/DeleteEventModal";

const getRoutes = () => {
  return (
    <Route path="timelines" component={TimelineRouteModal}>
      <IndexRoute component={TimelineIndexModal} />
      <Route path="new" component={NewTimelineModal} />
      <Route path=":timelineId" component={TimelineDetailsModal} />
      <Route path=":timelineId/edit" component={EditTimelineModal} />
      <Route path=":timelineId/archive" component={TimelineArchiveModal} />
      <Route path="new/events/new" component={NewEventWithTimelineModal} />
      <Route path=":timelineId/events/new" component={NewEventModal} />
      <Route
        path=":timelineId/events/:timelineEventId/edit"
        component={EditEventModal}
      />
      <Route
        path=":timelineId/events/:timelineEventId/delete"
        component={DeleteEventModal}
      />
    </Route>
  );
};

export default getRoutes;
