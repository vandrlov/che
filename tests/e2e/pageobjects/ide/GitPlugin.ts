import { injectable, inject } from 'inversify';
import { CLASSES } from '../../inversify.types';
import { DriverHelper } from '../../utils/DriverHelper';
import { TestConstants } from '../../TestConstants';
import { By } from 'selenium-webdriver';
import { Logger } from '../../utils/Logger';

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
export class GitPlugin {
    private static readonly COMMIT_MESSAGE_TEXTAREA: string = 'textarea#theia-scm-input-message';

    constructor(@inject(CLASSES.DriverHelper) private readonly driverHelper: DriverHelper) { }

    async openGitPluginContainer(timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('GitPlugin.openGitPluginContainer');

        const sourceCountrolGitBtn: string = '//li[@id=\'shell-tab-scm-view-container\' and @style[contains(.,\'height\')]]';
        await this.driverHelper.waitAndClick(By.xpath(sourceCountrolGitBtn), timeout);
        this.waitViewOfContainer();
    }

    async waitViewOfContainer(timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('GitPlugin.waitGitHubContainer');

        const gitHubContainerLocator: By = By.id('scm-view-container--scm-view');
        await this.driverHelper.waitVisibility(gitHubContainerLocator, timeout);
    }

    async waitCommitMessageTextArea(timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('GitPlugin.waitCommitMessageTextArea');

        const textArea: By = By.css(GitPlugin.COMMIT_MESSAGE_TEXTAREA);
        await this.driverHelper.waitVisibility(textArea, timeout);
    }

    async typeCommitMessage(commitMessage: string, timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('GitPlugin.typeCommitMessage');

        this.waitCommitMessageTextArea(timeout);
        await this.driverHelper.type(By.css(GitPlugin.COMMIT_MESSAGE_TEXTAREA), commitMessage, timeout);
    }

    async selectCommandInMoreActionsMenu(commandName: string, timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('GitPlugin.selectCommandInMoreActionsMenu');

        await this.clickOnMoreActions();
        this.driverHelper.waitAndClick(By.xpath(`//li[@data-command]/div[text()=\'${commandName}\']`), timeout);
    }

    async clickOnMoreActions() {
        Logger.debug('GitPlugin.clickOnMoreActions');

        await this.driverHelper.waitAndClick(By.id('__more__'));
    }

    async waitChangedFileInChagesList(expectedItem: string, timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('GitPlugin.waitChangedFileInChagesList');

        await this.driverHelper.waitPresence(By.xpath(`//div[@class='changesContainer']//span[text()=\'${expectedItem}\']`), timeout);
    }

    async waitStagedFileInStagedChanges(expectedStagedItem: string, timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('GitPlugin.waitStagedFileInStagedChanges');

        await this.driverHelper.waitPresence(By.xpath(`//div[text()='Staged Changes']/parent::div/following-sibling::div//span[text()=\'${expectedStagedItem}\']`), timeout);
    }

    async commitFromScmView(timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('GitPlugin.commitFromScmView');

        await this.driverHelper.waitAndClick(By.id('__scm-view-container_title:__plugin.scm.title.action.git.commit'), timeout);
    }

    async stageAllChanges(expectedStagedItem: string, timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('GitPlugin.stageAllChanges');

        await this.driverHelper.scrollTo(By.xpath('//div[@class=\'changesContainer\']//div[text()=\'Changes\']'), timeout);
        await this.driverHelper.waitAndClick(By.xpath('//a[@title=\'Stage All Changes\']'), timeout);
        this.waitStagedFileInStagedChanges(expectedStagedItem);
    }

    async waitDataIsSynchronized() {
        Logger.debug('GitPlugin.waitDataIsSynchronized');
        await this.driverHelper.waitDisappearance(By.xpath(`//div[contains(@title,'Synchronize Changes')]//span[contains(.,' 0↓')]`));
    }

}
