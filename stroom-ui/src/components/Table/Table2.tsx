import React, {
  createContext,
  forwardRef,
  PropsWithChildren,
  ReactElement,
} from "react";
import "./styles.css";
import { FixedSizeList as List } from "react-window";
import { TableProps } from "./Table";

const StickyListContext = createContext({
  ItemRenderer: {},
  stickyIndices: [],
  tableProps: {},
});

const Cells = ({ index, tableProps }) => {
  const row = tableProps.data[index];
  if (row) {
    return tableProps.columns.map((column) => {
      return <div key={column}>{row[column.accessor]}</div>;
    });
  }
  return null;
};

const ItemWrapper = ({ data, index, style }) => {
  const { ItemRenderer, stickyIndices, tableProps } = data;
  if (stickyIndices && stickyIndices.includes(index)) {
    return null;
  }

  return (
    <ItemRenderer index={index} style={style}>
      <Cells index={index} tableProps={tableProps} />
    </ItemRenderer>
  );
};

const Row = ({ index, style, children }) => (
  <div className="Table2__row" style={style}>
    {children}
  </div>
);

const StickyRow = ({ index, style, children }) => (
  <div className="Table2__sticky" style={style}>
    {children}
  </div>
);

const renderFunction = ({ children, ...rest }, ref) => (
  <StickyListContext.Consumer>
    {({ stickyIndices, tableProps }) => (
      <div ref={ref} {...rest}>
        {stickyIndices.map((index) => (
          <StickyRow
            index={index}
            key={index}
            style={{ top: index * 35, left: 0, width: "100%", height: 35 }}
          >
            {tableProps.columns.map((column) => {
              return column.Header;
            })}
          </StickyRow>
        ))}

        {children}
      </div>
    )}
  </StickyListContext.Consumer>
);
renderFunction.displayName = "StickyListContext";

const innerElementType = forwardRef(renderFunction);

const StickyList = ({ children, stickyIndices, tableProps, ...rest }) => (
  <StickyListContext.Provider
    value={{ ItemRenderer: children, stickyIndices, tableProps }}
  >
    <List
      itemData={{ ItemRenderer: children, stickyIndices, tableProps }}
      {...rest}
    >
      {ItemWrapper}
    </List>
  </StickyListContext.Provider>
);

// const rootElement = document.getElementById("root");
// render(
//   <StickyList
//     height={150}
//     innerElementType={innerElementType}
//     itemCount={1000}
//     itemSize={35}
//     stickyIndices={[0, 1]}
//     width={300}
//   >
//     {Row}
//   </StickyList>,
//   rootElement,
// );

export const Table2: React.FunctionComponent<TableProps<any>> = (
  tableProps,
) => (
  <StickyList
    height={150}
    innerElementType={innerElementType}
    itemCount={1000}
    itemSize={35}
    stickyIndices={[0]}
    width={300}
    tableProps={tableProps}
  >
    {Row}
  </StickyList>
);
