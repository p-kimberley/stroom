import { FunctionComponent } from "react";
import * as React from "react";
import FlexibleModal from "react-modal-touch";

export interface DialogProps {
  isOpen?: boolean;
  minWidth?: number;
  minHeight?: number;
  initWidth?: number;
  initHeight?: number;
  top?: number;
  left?: number;
  onRequestClose?: () => void;
  disableMove?: boolean;
  disableResize?: boolean;
  disableVerticalResize?: boolean;
  disableHorizontalResize?: boolean;
  disableVerticalMove?: boolean;
  disableHorizontalMove?: boolean;
  disableKeystroke?: boolean;
  onFocus?: () => void;
  className?: string;
}

export const ResizableDialog: FunctionComponent<DialogProps> = (props) => {
  const p = {
    isOpen: true,
    disableKeystroke: true,
    // onRequestClose: () => undefined,
    // onFocus={() => console.log("Modal is clicked")}
    // className={"my-modal-custom-class"}
    // initWidth: 900,
    // initHeight: 400,
    ...props,
  };
  return (
    <FlexibleModal {...p} aria-labelledby="contained-modal-title-vcenter">
      <div className="modal-content">{props.children}</div>
    </FlexibleModal>
  );
};
