/*
 * Copyright 2018 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { lifecycle } from 'recompose';
import { Provider } from 'react-redux';

import store from 'startup/store';

export const ReduxDecorator = storyFn => <Provider store={store}>{storyFn()}</Provider>;

const enhanceLocal = lifecycle({
  componentDidMount() {
    this.props.storeInit(this.props.store);
  },
});

const ReduxWithInit = enhanceLocal(({ children }) => <div className="fill-space">{children}</div>);

export const ReduxDecoratorWithInitialisation = storeInit => storyFn => (
  <Provider store={store}>
    <ReduxWithInit storeInit={storeInit} store={store}>
      {storyFn()}
    </ReduxWithInit>
  </Provider>
);