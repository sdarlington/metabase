import React, { cloneElement, ReactNode, useCallback } from "react";
import { isElement } from "react-is";
import { Route } from "react-router";
import Modal from "metabase/components/Modal";

export interface TimelineRouteModalProps {
  route?: Route;
  location?: Location;
  children?: ReactNode;
  onClose?: (route?: Route, location?: Location) => void;
}

const TimelineRouteModal = ({
  route,
  location,
  children,
  onClose,
}: TimelineRouteModalProps): JSX.Element | null => {
  const handleClose = useCallback(() => {
    onClose?.(route, location);
  }, [route, location, onClose]);

  return (
    <Modal full={false} onClose={handleClose}>
      {isElement(children) && cloneElement(children, { onClose: handleClose })}
    </Modal>
  );
};

export default TimelineRouteModal;
