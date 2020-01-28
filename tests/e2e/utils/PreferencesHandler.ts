import { injectable, inject } from 'inversify';
import 'reflect-metadata';
import { CLASSES } from '../inversify.types';
import { RequestHandler } from './RequestHandler';


@injectable()
export class PreferencesHandler {

    constructor(@inject(CLASSES.RequestHandler) private readonly requestHandler: RequestHandler) {
    }


    public async setTerminalToDom() {
        if (await this.isSetToDom()) {
            console.log('User preferences are already set to use terminal as a DOM.');
        } else {
            console.log('Setting user preferences to use terminal as a DOM.');
            await this.setToDom();
        }

        this.requestHandler.ttt();
    }

    private async isSetToDom(): Promise<boolean> {
        //    // let requestHandler: RequestHandler = e2eContainer.get(CLASSES.RequestHandler);
        //    const response = await this.requestHandler.processRequest(RequestType.GET, `${TestConstants.TS_SELENIUM_BASE_URL}/api/preferences`);
        //    let responseString = JSON.stringify(response.data);
        //    if ( responseString.includes('"terminal.integrated.rendererType":"dom"') ) {
        //          return true;
        //     }
        return false;
    }

    private setToDom() {
        console.log('Setting to dom...');
    }

}
