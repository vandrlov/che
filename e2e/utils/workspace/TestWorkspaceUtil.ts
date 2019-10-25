/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import { TestConstants } from '../../TestConstants';
import { injectable, inject } from 'inversify';
import { DriverHelper } from '../DriverHelper';
import { CLASSES } from '../../inversify.types';
import 'reflect-metadata';
import * as rm from 'typed-rest-client/RestClient';
import { WorkspaceStatus } from './WorkspaceStatus';
import { ITestWorkspaceUtil } from './ITestWorkspaceUtil';
import axios from 'axios';
import querystring from 'querystring';

@injectable()
export class TestWorkspaceUtils implements ITestWorkspaceUtil {

    constructor(@inject(CLASSES.DriverHelper) private readonly driverHelper: DriverHelper,
        private readonly rest: rm.RestClient = new rm.RestClient('rest-samples')) {
        rest = new rm.RestClient('rest-samples');
    }

    public async waitWorkspaceStatus(namespace: string, workspaceName: string, expectedWorkspaceStatus: WorkspaceStatus) {
        const workspaceStatusApiUrl: string = `${TestConstants.TS_SELENIUM_BASE_URL}/api/workspace/${namespace}:${workspaceName}`;
        const attempts: number = TestConstants.TS_SELENIUM_WORKSPACE_STATUS_ATTEMPTS;
        const polling: number = TestConstants.TS_SELENIUM_WORKSPACE_STATUS_POLLING;

        for (let i = 0; i < attempts; i++) {
            const response: rm.IRestResponse<any> = await this.rest.get(workspaceStatusApiUrl, { additionalHeaders: { 'Authorization': 'Bearer ' + this.getAuthToken() } });

            if (response.statusCode !== 200) {
                await this.driverHelper.wait(polling);
                continue;
            }

            const workspaceStatus: string = await response.result.status;

            if (workspaceStatus === expectedWorkspaceStatus) {
                return;
            }

            await this.driverHelper.wait(polling);
        }

        throw new Error(`Exceeded the maximum number of checking attempts, workspace status is different to '${expectedWorkspaceStatus}'`);
    }

    public async waitPluginAdding(namespace: string, workspaceName: string, pluginName: string) {
        const workspaceStatusApiUrl: string = `${TestConstants.TS_SELENIUM_BASE_URL}/api/workspace/${namespace}:${workspaceName}`;
        const attempts: number = TestConstants.TS_SELENIUM_PLUGIN_PRECENCE_ATTEMPTS;
        const polling: number = TestConstants.TS_SELENIUM_PLUGIN_PRECENCE_POLLING;

        for (let i = 0; i < attempts; i++) {
            const response: rm.IRestResponse<any> = await this.rest.get(workspaceStatusApiUrl, { additionalHeaders: { 'Authorization': 'Bearer ' + await this.getAuthToken() } });

            if (response.statusCode !== 200) {
                await this.driverHelper.wait(polling);
                continue;
            }

            const machines: string = JSON.stringify(response.result.runtime.machines);
            const isPluginPresent: boolean = machines.search(pluginName) > 0;

            if (isPluginPresent) {
                break;
            }

            if (i === attempts - 1) {
                throw new Error(`Exceeded maximum tries attempts, the '${pluginName}' plugin is not present in the workspace runtime.`);
            }

            await this.driverHelper.wait(polling);
        }
    }

    public async getListOfWorkspaceId() {
        const getAllWorkspacesApiUrl: string = `${TestConstants.TS_SELENIUM_BASE_URL}/api/workspace`;
        const getAllWorkspacesResponse: rm.IRestResponse<any> = await this.rest.get(getAllWorkspacesApiUrl, { additionalHeaders: { 'Authorization': 'Bearer ' + await this.getAuthToken() } });
        interface IMyObj {
            id: string;
            status: string;
        }
        let stringified = JSON.stringify(getAllWorkspacesResponse.result);
        let arrayOfWorkspaces = <IMyObj[]>JSON.parse(stringified);
        let wsList: Array<string> = [];
        for (let entry of arrayOfWorkspaces) {
            wsList.push(entry.id);
        }
        return wsList;
    }

    public async getIdOfRunningWorkspaces(): Promise<Array<string>> {
        const getAllWorkspacesApiUrl: string = `${TestConstants.TS_SELENIUM_BASE_URL}/api/workspace`;
        const getAllWorkspacesResponse: rm.IRestResponse<any> = await this.rest.get(getAllWorkspacesApiUrl, { additionalHeaders: { 'Authorization': 'Bearer ' + await this.getAuthToken() } });
        interface IMyObj {
            id: string;
            status: string;
        }
        let stringified = JSON.stringify(getAllWorkspacesResponse.result);
        let arrayOfWorkspaces = <IMyObj[]>JSON.parse(stringified);
        let idOfRunningWorkspace: Array<string> = new Array();

        for (let entry of arrayOfWorkspaces) {
            if (entry.status === 'RUNNING') {
                idOfRunningWorkspace.push(entry.id);
            }
        }

        return idOfRunningWorkspace;
    }

    public async removeWorkspaceById(id: string) {
        const getInfoURL: string = `${TestConstants.TS_SELENIUM_BASE_URL}/api/workspace/${id}`;
        const attempts: number = TestConstants.TS_SELENIUM_PLUGIN_PRECENCE_ATTEMPTS;
        const polling: number = TestConstants.TS_SELENIUM_PLUGIN_PRECENCE_POLLING;
        let stopped: Boolean = false;

        for (let i = 0; i < attempts; i++) {
            const getInfoResponse: rm.IRestResponse<any> = await this.rest.get(getInfoURL, { additionalHeaders: { 'Authorization': 'Bearer ' + await this.getAuthToken() } });
            if (getInfoResponse.result.status === 'STOPPED') {
                stopped = true;
                break;
            }
            await this.driverHelper.wait(polling);
        }

        if (stopped) {
            const deleteWorkspaceApiUrl: string = `${TestConstants.TS_SELENIUM_BASE_URL}/api/workspace/${id}`;
            const deleteWorkspaceResponse: rm.IRestResponse<any> = await this.rest.del(deleteWorkspaceApiUrl, { additionalHeaders: { 'Authorization': 'Bearer ' + await this.getAuthToken() } });

            // response code 204: "No Content" expected
            if (deleteWorkspaceResponse.statusCode !== 204) {
                throw new Error(`Can not remove workspace. Code: ${deleteWorkspaceResponse.statusCode} Result: ${deleteWorkspaceResponse.result}`);
            }
        } else {
            throw new Error(`Can not remove workspace with id ${id}, because it is still not in STOPPED state.`);
        }
    }

    async getAuthToken(): Promise<string> {
        let result = TestConstants.TS_SELENIUM_MULTIUSER ? await this.getCheBearerToken() : 'dummy_token';
        return result;
    }

    async getCheBearerToken(): Promise<string> {
        const keycloakAuthSuffix = '/auth/realms/che/protocol/openid-connect/token';
        const keycloakUrl = TestConstants.TS_SELENIUM_BASE_URL.replace('che', 'keycloak') + keycloakAuthSuffix;
        const params = {
            client_id: 'che-public',
            username: TestConstants.TS_SELENIUM_USERNAME,
            password: TestConstants.TS_SELENIUM_PASSWORD,
            grant_type: 'password'
        };
        const responseToObtainBearerToken = await axios.post(keycloakUrl, querystring.stringify(params));
        return responseToObtainBearerToken.data.access_token;
    }

    public async stopWorkspaceById(id: string) {
        const stopWorkspaceApiUrl: string = `${TestConstants.TS_SELENIUM_BASE_URL}/api/workspace/${id}/runtime`;
        const stopWorkspaceResponse: rm.IRestResponse<any> = await this.rest.del(stopWorkspaceApiUrl, { additionalHeaders: { 'Authorization': 'Bearer ' + await this.getAuthToken() } });

        // response code 204: "No Content" expected
        if (stopWorkspaceResponse.statusCode !== 204) {
            throw new Error(`Can not stop workspace. Code: ${stopWorkspaceResponse.statusCode} Result: ${stopWorkspaceResponse.result}`);
        }
    }

    public async cleanUpAllWorkspaces() {
        let d = new Date();
        console.log("Cleaning workspaces at " + d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds())
        let listOfRunningWorkspaces: Array<string> = await this.getIdOfRunningWorkspaces();
        for (const entry of listOfRunningWorkspaces) {
            await this.stopWorkspaceById(entry);
        }

        let listAllWorkspaces: Array<string> = await this.getListOfWorkspaceId();

        for (const entry of listAllWorkspaces) {
            this.removeWorkspaceById(entry);
        }

    }

    removeWorkspace(namespace: string, workspaceId: string): void {
        throw new Error('Method not implemented.');
    }

    stopWorkspace(namespace: string, workspaceId: string): void {
        throw new Error('Method not implemented.');
    }

}
