/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import { e2eContainer } from '../../inversify.config';
import { CLASSES, TYPES } from '../../inversify.types';
import { Ide } from '../../pageobjects/ide/Ide';
import { ProjectTree } from '../../pageobjects/ide/ProjectTree';
import { TestConstants } from '../../TestConstants';
import { DriverHelper } from '../../utils/DriverHelper';
import { ICheLoginPage } from '../../pageobjects/login/ICheLoginPage';
import { TestWorkspaceUtils } from '../../utils/workspace/TestWorkspaceUtil';
const driverHelper: DriverHelper = e2eContainer.get(CLASSES.DriverHelper);
const ide: Ide = e2eContainer.get(CLASSES.Ide);
const projectTree: ProjectTree = e2eContainer.get(CLASSES.ProjectTree);
const factoryUrl: string = `${TestConstants.TS_SELENIUM_BASE_URL}/f?url=https://raw.githubusercontent.com/redhat-developer-demos/guru-night/master/quarkus-tutorial/devfile.yaml`;
const cheLoginPage: ICheLoginPage = e2eContainer.get<ICheLoginPage>(TYPES.CheLogin);
const testWorkspaceUtils: TestWorkspaceUtils = e2eContainer.get<TestWorkspaceUtils>(TYPES.WorkspaceUtil);




suite('Load test suite', async () => {

    suiteTeardown (async function () { await testWorkspaceUtils.cleanUpAllWorkspaces(); });

    test('Login and navigate to factory url', async () => {
        await driverHelper.navigateToUrl(factoryUrl);
        await cheLoginPage.login();
    });

    test('Wait loading workspace and get time', async () => {
        await ide.waitAndSwitchToIdeFrame();
        await projectTree.openProjectTreeContainer();

    });

});


