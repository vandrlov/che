import { injectable } from 'inversify';
import axios from 'axios';
import { TestConstants } from '../../TestConstants';
import querystring from 'querystring';
import { ThenableWebDriver } from 'selenium-webdriver';

@injectable()
export class KeyCloakUtil {

    /**
     * Get keycloak token with API
     */
    async getBearerToken(): Promise<string> {
        let keycloakUrl = '';
        try {
            const cheKeycloakTokenEndpoint = 'che.keycloak.token.endpoint';
            const keycloakEndpoint = await axios.get(TestConstants.TS_SELENIUM_BASE_URL + '/api/keycloak/settings');
            keycloakUrl = keycloakEndpoint.data[cheKeycloakTokenEndpoint];

            const cheKeycloakClientId = 'che.keycloak.client_id';
            const keycloakClientId = keycloakEndpoint.data[cheKeycloakClientId];
            const params = {
                client_id: keycloakClientId,
                password: TestConstants.TS_SELENIUM_USERNAME,
                username: TestConstants.TS_SELENIUM_PASSWORD,
                grant_type: 'password'
            };

            const responseToObtainedBearerToken = await axios.post(keycloakUrl, querystring.stringify(params));
            return TestConstants.TS_SELENIUM_MULTIUSER ? 'Bearer ' + responseToObtainedBearerToken.data.access_token : 'dummy_token';
        } catch (error) {
            console.error('Can not get bearer token. URL used: ' + keycloakUrl + error);
            throw error;
        }
    }

    /**
     * Get keycloak token with javascript invocation for the current Webdriver session
     * Use carefully! In multiuser mode a user should be loggened in a Workspace
     * @param webdriverInstance current Webdriver instance
     */
    async getBearerTokenFromBrowserSession(webdriverInstance: ThenableWebDriver): Promise<string> {
        try {await webdriverInstance.switchTo().defaultContent();
        const obtainedToken: string = await webdriverInstance.executeScript('return window._keycloak.token');
        return TestConstants.TS_SELENIUM_MULTIUSER ? 'Bearer ' + obtainedToken : 'dummy_token'; } catch (error) {
            console.log('Cannot get bearer token from the current webdriver session ' );
            console.error(error);
            throw error;
        }
    }

}
