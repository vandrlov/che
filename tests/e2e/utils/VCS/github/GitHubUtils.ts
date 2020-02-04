import { injectable } from 'inversify';
import axios from 'axios';

@injectable()
export class GitHubUtils {
  private static readonly GITHUB_API_ENTRIPOINT_URL = 'https://api.github.com/';

  async addPublicSshKeyToUserAccount(authToken: string, title: string, key: string) {
    const gitHubApiSshURL: string = GitHubUtils.GITHUB_API_ENTRIPOINT_URL + 'user/keys';
    const authHeader = { headers: { 'Authorization': 'token ' + authToken, 'Content-Type': 'application/json' } };

    const data = {
      title: `${title}`,
      key: `${key}`
    };

    try { await axios.post(gitHubApiSshURL, JSON.stringify(data), authHeader); } catch (error) {
      console.error('Cannot add the public key to the GitHub account: ');
      console.error(error);
      throw error;
    }
  }

  async getRawContentFromFile(pathToFile: string): Promise<string> {
    const gitHubContentEntryPointUrl: string = 'https://raw.githubusercontent.com/';
    const pathToRawContent: string = `${gitHubContentEntryPointUrl}${pathToFile}`;
    const authorization: string = 'Authorization';
    const contentType: string = 'Content-Type'

    try {
      delete axios.defaults.headers.common[authorization];
      delete axios.defaults.headers.common[contentType];
      const response = await axios.get(`${gitHubContentEntryPointUrl}${pathToFile}`);
      return response.data;
    } catch (error) {
      console.error('Cannot get content form the raw github content: ' + pathToRawContent);
      console.error(error);
      throw error;
    }
  }

}
