import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { compose, withProps, branch, renderComponent, withHandlers } from 'recompose';
import { Grid, Header, Icon, Button } from 'semantic-ui-react';
import { withRouter } from 'react-router-dom';

import Loader from 'components/Loader'
import Tooltip from 'components/Tooltip';
import AppSearchBar from 'components/AppSearchBar';
import DocRefBreadcrumb from 'components/DocRefBreadcrumb';
import { findItem } from 'lib/treeUtils';
import { actionCreators } from './redux';
import { fetchDocInfo } from 'components/FolderExplorer/explorerClient';
import DndDocRefListingEntry from './DndDocRefListingEntry';
import NewDocDialog from './NewDocDialog';
import DocRefInfoModal from 'components/DocRefInfoModal';
import withDocumentTree from './withDocumentTree';
import withSelectableItemListing, { SELECTION_BEHAVIOUR } from 'lib/withSelectableItemListing';

const {
  prepareDocRefCreation,
  prepareDocRefDelete,
  prepareDocRefCopy,
  prepareDocRefRename,
  prepareDocRefMove,
} = actionCreators;

const LISTING_ID = 'folder-explorer';

const enhance = compose(
  withDocumentTree,
  withRouter,
  withHandlers({
    openDocRef: ({ history }) => d => history.push(`/s/doc/${d.type}/${d.uuid}`)
  }),
  connect(
    ({ folderExplorer: { documentTree }, selectableItemListings }, { folderUuid }) => ({
      folder: findItem(documentTree, folderUuid),
      selectableItemListing: selectableItemListings[LISTING_ID] || {},
    }),
    {
      prepareDocRefCreation,
      prepareDocRefDelete,
      prepareDocRefCopy,
      prepareDocRefRename,
      prepareDocRefMove,
      fetchDocInfo,
    },
  ),
  branch(({ folder }) => !folder, renderComponent(() => <Loader message="Loading folder..." />)),
  withSelectableItemListing(({ openDocRef, folder: { lineage, node: { children } } }) => ({
    listingId: LISTING_ID,
    items: children,
    selectionBehaviour: SELECTION_BEHAVIOUR.MULTIPLE,
    openItem: openDocRef,
    goBack: () => {
      if (lineage.length > 0) {
        openDocRef(lineage[lineage.length - 1])
      }
    }
  })),
  withProps(({
    folder,
    prepareDocRefCreation,
    prepareDocRefDelete,
    prepareDocRefCopy,
    prepareDocRefRename,
    prepareDocRefMove,
    fetchDocInfo,
    selectableItemListing: { selectedItems, items },
  }) => {
    const actionBarItems = [
      {
        icon: 'file',
        onClick: () => prepareDocRefCreation(folder.node),
        tooltip: 'Create a Document',
      },
    ];

    const singleSelectedDocRef = selectedItems.length === 1 ? selectedItems[0] : undefined;
    const selectedDocRefUuids = selectedItems.map(d => d.uuid);

    if (selectedItems.length > 0) {
      if (singleSelectedDocRef) {
        actionBarItems.push({
          icon: 'info',
          onClick: () => fetchDocInfo(singleSelectedDocRef),
          tooltip: 'View Information about this document',
        });
        actionBarItems.push({
          icon: 'pencil',
          onClick: () => prepareDocRefRename(singleSelectedDocRef),
          tooltip: 'Rename this document',
        });
      }
      actionBarItems.push({
        icon: 'copy',
        onClick: d => prepareDocRefCopy(selectedDocRefUuids),
        tooltip: 'Copy selected documents',
      });
      actionBarItems.push({
        icon: 'move',
        onClick: () => prepareDocRefMove(selectedDocRefUuids),
        tooltip: 'Move selected documents',
      });
      actionBarItems.push({
        icon: 'trash',
        onClick: () => prepareDocRefDelete(selectedDocRefUuids),
        tooltip: 'Delete selected documents',
      });
    }

    return { actionBarItems };
  }),
);

const FolderExplorer = ({
  folder: { node },
  folderUuid,
  actionBarItems,
  openDocRef,
  onKeyDownWithShortcuts,
}) => (
    <React.Fragment>
      <Grid className="content-tabs__grid">
        <Grid.Column width={16}>
          <AppSearchBar className='app-search-bar' onChange={openDocRef} />
        </Grid.Column>
        <Grid.Column width={11}>
          <Header as="h3">
            <Icon name="folder" />
            <Header.Content className="header">{node.name}</Header.Content>
            <Header.Subheader>
              <DocRefBreadcrumb docRefUuid={node.uuid} openDocRef={openDocRef} />
            </Header.Subheader>
          </Header>
        </Grid.Column>
        <Grid.Column width={5}>
          <span className="doc-ref-listing-entry__action-bar">
            {actionBarItems.map(({ onClick, icon, tooltip }, i) => (
              <Tooltip
                key={i}
                trigger={
                  <Button
                    className="icon-button"
                    circular
                    onClick={onClick}
                    icon={icon}
                  />
                }
                content={tooltip}
              />
            ))}
          </span>
        </Grid.Column>
      </Grid>
      <div className="doc-ref-listing" tabIndex={0} onKeyDown={onKeyDownWithShortcuts}>
        {node.children.map((docRef, index) => (
          <DndDocRefListingEntry
            key={docRef.uuid}
            index={index}
            listingId={LISTING_ID}
            docRefUuid={docRef.uuid}
            onNameClick={openDocRef}
            openDocRef={openDocRef}
          />
        ))}
      </div>

      <DocRefInfoModal />
      <NewDocDialog />
    </React.Fragment>
  );

const EnhanceFolderExplorer = enhance(FolderExplorer);

EnhanceFolderExplorer.contextTypes = {
  store: PropTypes.object,
};

EnhanceFolderExplorer.propTypes = {
  folderUuid: PropTypes.string.isRequired,
};

export default EnhanceFolderExplorer;