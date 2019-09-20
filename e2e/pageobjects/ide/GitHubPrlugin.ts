import { injectable, inject } from 'inversify';
import { CLASSES } from '../../inversify.types';
import { DriverHelper } from '../../utils/DriverHelper';
import { TestConstants } from '../../TestConstants';
import { By } from 'selenium-webdriver';
import { Ide } from './Ide';

/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

@injectable()
export class GitHubPrlugin {
    constructor(@inject(CLASSES.DriverHelper) private readonly driverHelper: DriverHelper,
        @inject(CLASSES.Ide) private readonly ide: Ide) { }
    private static readonly OCTOCAT_ICON_ID = 'shell-tab-plugin-view-container:github-pull-requests';
    private static readonly PR_DOC_PANEL_ID = 'plugin-view-container:github-pull-requests';

    waitAndClickOnOctocatIcon(timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        this.driverHelper.waitAndClick(By.id(GitHubPrlugin.OCTOCAT_ICON_ID), timeout);
    }

    waitPullRequestDocPanelIsOpened(timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        this.driverHelper.waitVisibility(By.id(GitHubPrlugin.PR_DOC_PANEL_ID), timeout);
    }

}
