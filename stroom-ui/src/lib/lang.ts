/*
 * Copyright 2017 Crown Copyright
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
// import dateFormat from "dateformat";

function hasAnyProps(object: Record<string, unknown>): boolean {
  let hasProps = false;
  for (const prop in object) {
    if (Object.prototype.hasOwnProperty.call(object, prop)) {
      hasProps = true;
    }
  }
  return hasProps;
}

// const formatDate = (dateString: string) => {
//   const dateFormatString = "ddd mmm d yyyy, hh:MM:ss";
//   return dateString ? dateFormat(dateString, dateFormatString) : "";
// };

export { hasAnyProps };
