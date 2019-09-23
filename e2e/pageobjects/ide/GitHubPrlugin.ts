import { injectable, inject } from 'inversify';
import { CLASSES } from '../../inversify.types';
import { DriverHelper } from '../../utils/DriverHelper';
import { TestConstants } from '../../TestConstants';
import { By } from 'selenium-webdriver';





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
    public static readonly MAIN_ITEMS_LIST_LOCATOR = {All: '//div[contains(@id, \':All\')]', CreatedByMe: '//div[contains(@id, \':Created By Me\')]', AssignedToMe: '//div[contains(@id, \':Assigned To Me\')]', WaitingForMyReview: '//div[contains(@id, \':Waiting For My Review\')]', LocalPullRequestBranches: '//div[contains(@id, \':Local Pull Request Branches\')]'};
    private static readonly OCTOCAT_ICON_ID: string = 'shell-tab-plugin-view-container:github-pull-requests';
    private static readonly PR_DOC_PANEL_ID: string = 'plugin-view-container:github-pull-requests';

    constructor(@inject(CLASSES.DriverHelper) private readonly driverHelper: DriverHelper) { }


    async waitAndClickOnOctocatIcon(timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        await this.driverHelper.waitAndClick(By.id(GitHubPrlugin.OCTOCAT_ICON_ID), timeout);
    }

    async waitPullRequestDocPanelIsOpened(timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        await this.driverHelper.waitVisibility(By.id(GitHubPrlugin.PR_DOC_PANEL_ID), timeout);

    }


}
