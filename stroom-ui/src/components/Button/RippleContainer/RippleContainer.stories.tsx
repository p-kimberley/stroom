import * as React from "react";
import { storiesOf } from "@storybook/react";
import { RippleContainer, useRipple } from "./RippleContainer";

const TestHarness: React.FunctionComponent = () => {
  const onClick: React.MouseEventHandler<HTMLButtonElement> = React.useCallback(
    () => console.log("Clicked"),
    [],
  );
  const { onClickWithRipple, ripples } = useRipple(onClick);

  return (
    <button
      className="control"
      onClick={onClickWithRipple}
      style={{
        width: "200px",
        height: "200px",
        overflow: "hidden",
        position: "relative",
      }}
    >
      <RippleContainer ripples={ripples} />
    </button>
  );
};

storiesOf("General Purpose/Button", module).add("Ripple Container", () => (
  <TestHarness />
));
