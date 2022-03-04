import { connect } from "react-redux";
import { Route } from "react-router";
import { push } from "react-router-redux";
import { getParentPath } from "metabase/hoc/ModalRoute";
import TimelineRouteModal from "../../components/TimelineRouteModal";

const mapDispatchToProps = (dispatch: any) => ({
  onClose: (route?: Route, location?: Location) => {
    dispatch(push(getParentPath(route, location)));
  },
});

export default connect(null, mapDispatchToProps)(TimelineRouteModal);
