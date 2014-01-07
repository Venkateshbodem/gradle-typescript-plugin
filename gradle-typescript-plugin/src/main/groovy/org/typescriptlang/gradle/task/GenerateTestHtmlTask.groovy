package org.typescriptlang.gradle.task

import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction
import org.typescriptlang.gradle.util.PathsUtil
import org.typescriptlang.gradle.TypeScriptPluginExtension

import java.util.regex.Matcher
import java.util.regex.Pattern

class GenerateTestHtmlTask extends TypeScriptPluginTask {

    @Override
    void setupInputsAndOutputs(TypeScriptPluginExtension extension) {
        inputs.dir(extension.getMainSourceCopyForTestDir())
        inputs.dir(extension.getTestSourceCopyForTestDir())
        outputs.file(new File(extension.getSourceCopyForTestDir(), "console-test.html"))
        outputs.file(new File(extension.getSourceCopyForTestDir(), "browser-test.html"))
    }

    @TaskAction
    void generate() {
        TypeScriptPluginExtension extension = TypeScriptPluginExtension.getInstance(project)
        File testHtmlDir = extension.getTestHtmlDir()
        testHtmlDir.mkdirs()

        File consoleTestTemplate = File.createTempFile("console-test-template", "html");
        consoleTestTemplate.deleteOnExit();
        consoleTestTemplate << GenerateTestHtmlTask.class.getResourceAsStream("/test-resources/template/console-test.html.template")
        File browserTestTemplate = File.createTempFile("browser-test-template", "html");
        browserTestTemplate.deleteOnExit();
        browserTestTemplate << GenerateTestHtmlTask.class.getResourceAsStream("/test-resources/template/browser-test.html.template")

        File testSourcesDir = extension.getTestSourceCopyForTestDir()

        def FileTree testTsFilesTree = project.fileTree(testSourcesDir).include('**/*.ts').exclude("**/*.d.ts")
        def testTsFiles = []
        testTsFilesTree.visit({
            fileVisitDetails -> if (!fileVisitDetails.isDirectory()) {
                testTsFiles.add(fileVisitDetails.getRelativePath())
            }
        })



        File requireJsConfigFile = extension.getRequireJsConfigFileInSourceCopyDir()
        Map<String, Object> templateVariableValues = [
                testFiles: testTsFiles,
                testJsLibs: extension.getTestLibsDir().path,
                requireJsConfigFile: requireJsConfigFile.path,
                requireJsConfigFilePathRelativeFromBuild: PathsUtil.getRelativePath(requireJsConfigFile.parentFile, project.getBuildDir()),
                testSourcesPath: testSourcesDir.path
        ]
        project.copy {CopySpec copySpec ->
            copySpec.into testHtmlDir
            from browserTestTemplate, consoleTestTemplate
            rename {
                String originalName ->
                    // rename temporary file with random name "console-test-template-4s5h7ds.html" to "console-test.html", same for browser-test-...
                    Matcher filenameMatcher = Pattern.compile("^([a-z]+-test)-template.*").matcher(originalName);
                    filenameMatcher.find();
                    return filenameMatcher.group(1) + ".html";
            }
            expand(templateVariableValues)
        }

    }
}
