import { NameGenerator } from '../../utils/NameGenerator';
import 'reflect-metadata';
import * as projectAndFileTests from '../../testsLibrary/ProjectAndFileTests';
import * as workspaceHandling from '../../testsLibrary/WorksapceHandlingTests';
import * as codeExecutionTests from '../../testsLibrary/CodeExecutionTests';
import * as commonLsTests from '../../testsLibrary/LsTests';

const workspaceName: string = NameGenerator.generate('wksp-test-', 5);
const workspaceStack: string = 'Python';
const workspaceSampleName: string = 'python-hello-world';
const fileFolderPath: string = `${workspaceSampleName}`;
const tabTitle: string = 'hello-world.py';

suite(`${workspaceStack} test`, async () => {

    suite(`Create ${workspaceStack} workspace ${workspaceName}`, async () => {
        workspaceHandling.createAndOpenWorkspace(workspaceName, workspaceStack);
        projectAndFileTests.waitWorkspaceReadiness(workspaceName, workspaceSampleName, 'hello-world.py');
    });


    suite(' Open workspace and run task', async () => {
        projectAndFileTests.openFile(fileFolderPath, tabTitle)
        codeExecutionTests.runTask('run', 10000);
        codeExecutionTests.closeTerminal('run');
    });

    suite('Language server validation', async () => {
        commonLsTests.suggestionInvoking(tabTitle, 8, 2, 'str');
        commonLsTests.autocomplete(tabTitle, 9, 2, 'print');
        commonLsTests.errorHighlighting(tabTitle, 'print msg ', 9);
    });

    
    suite ('Stop and remove workspace', async() => {
        workspaceHandling.stopWorkspace(workspaceName);
        workspaceHandling.removeWorkspace(workspaceName);
    });
});
