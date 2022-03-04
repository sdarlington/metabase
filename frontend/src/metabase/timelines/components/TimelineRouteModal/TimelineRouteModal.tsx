import React, { cloneElement, ReactElement } from "react";
import Modal from "metabase/components/Modal";

export interface TimelineRouteModalProps {
  children?: ReactElement;
  onClose?: () => void;
}

const TimelineRouteModal = ({
  children,
  onClose,
}: TimelineRouteModalProps): JSX.Element | null => {
  return (
    <Modal full={false}>
      {children && cloneElement(children, { onClose })}
    </Modal>
  );
};

export default TimelineRouteModal;
