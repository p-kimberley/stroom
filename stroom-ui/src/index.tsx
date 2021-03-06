import * as React from "react";
import * as ReactDOM from "react-dom";

import { DragDropContext } from "react-dnd";
import HTML5Backend from "react-dnd-html5-backend";

import { Routes } from "components/AppChrome";
import setupFontAwesome from "lib/setupFontAwesome";

import { ThemeContextProvider } from "lib/useTheme/useTheme";
import { CustomRouter } from "lib/useRouter";

import { createBrowserHistory as createHistory } from "history";
import ConfigProvider from "startup/config/ConfigProvider";

import "styles/main.scss";
import { DocumentTreeContextProvider } from "components/DocumentEditors/api/explorer";
import { ErrorReportingContextProvider } from "components/ErrorPage";
import { PromptDisplayBoundary } from "./components/Prompt/PromptDisplayBoundary";
export const history = createHistory();

const DndRoutes = DragDropContext(HTML5Backend)(Routes);

setupFontAwesome();

const App: React.FunctionComponent = () => (
  <ErrorReportingContextProvider>
    <PromptDisplayBoundary>
      <ConfigProvider>
        <ThemeContextProvider>
          <CustomRouter history={history}>
            <DocumentTreeContextProvider>
              <DndRoutes />
            </DocumentTreeContextProvider>
          </CustomRouter>
        </ThemeContextProvider>
      </ConfigProvider>
    </PromptDisplayBoundary>
  </ErrorReportingContextProvider>
);

ReactDOM.render(<App />, document.getElementById("root") as HTMLElement);
