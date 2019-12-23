import { injectable, inject } from 'inversify';
import { TestConstants } from '../../TestConstants';
import { CLASSES } from '../../inversify.types';
import { KeyCloakUtils } from '../../utils/keycloak/KeyCloakUtils';
import axios from 'axios';

@injectable()
export class CheGitApi {
  static readonly GIT_API_ENTRIPOINT_URL = TestConstants.TS_SELENIUM_BASE_URL + '/api/ssh/vcs';

  constructor(@inject(CLASSES.KeyCloakUtils) private readonly keyCloak: KeyCloakUtils) { }



  public async  getPublicSSHKey(): Promise<string> {
    const bearerToken: string = await this.keyCloak.getBearerToken();
    const headerParams = { headers: { 'Authorization': bearerToken } };
    try {
      const responce = await axios.get(CheGitApi.GIT_API_ENTRIPOINT_URL, headerParams);
      return responce.data[0].publicKey;
    } catch (error) {
      console.error('Cannot get public ssh key with API \n' + error);
      throw error;
    }

  }

}
