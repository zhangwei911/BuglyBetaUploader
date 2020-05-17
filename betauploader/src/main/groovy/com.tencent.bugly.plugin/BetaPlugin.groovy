package com.tencent.bugly.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import groovy.json.JsonSlurper

/**
 * {@code BetaPlugin} is a gradle plugin for uploading apk file to Bugly platform
 *
 * <p> The plugin will create a task "upload${variantName}BetaApkFile.
 * ({@code "variantname"} means the name of variant. e.g.,"Release")  for doing following things:
 * <p> 1.upload apk file to bugly platform.
 *
 * if you want to upload release apk file ,you must set the signingConfigs on build.gradle, like this:
 *  signingConfigs {*    release {*          storeFile file("your keystore file")
 *          storePassword "your keystore password"
 *          keyAlias "your key alias"
 *          keyPassword "your key password"
 *}*}*
 * <p>The plugin should be configured through the following required properties:
 * <p>{@code appId}: app ID of Bugly platform.
 * <p>{@code appKey}: app Key of Bugly platform.
 * <p>Other optional properties:
 * <p>{@code title}: title of the version
 * <p>{@code desc}: description of the version
 * <p>{@code enable}: switch for controlling execution of "upload${variantName}BetaApkFile".
 * <p>{@code apkFile}: the file path you want to set
 * <p>{@code secret}: the open range 1: all 2:password 4:administrator 5: QQ group 6: white list
 * <p>{@code users}: if the open range was QQ group, you should set users like this : users = "group num1group num2, ..."
 * if the open range was white list, you should set users like this: users = "qq num1;qq num2;...", other scene no need to set
 * <p>{@code password}: if you set secret to 2, you should set the password
 * <p>{@code download_limit}: download limit, default 1000
 * <p>{@code expId}: the apk id you has been upload
 * @author wenjiewu
 */
public class BetaPlugin implements Plugin<Project> {
    private Project project = null;

    @Override
    void apply(Project project) {
        this.project = project
        // 接收外部参数
        project.extensions.create("beta", BetaExtension)

        // 取得外部参数
        if (project.android.hasProperty("applicationVariants")) { // For android application.
            project.android.applicationVariants.all { variant ->
                String variantName = variant.name.capitalize()
                // Check for execution
                if (false == project.beta.enable) {
                    project.logger.error("Bugly: beta gradle enable is false, if you want to auto upload apk file, you should set the execute = true")
                    return
                }
                variant.outputs.all { output ->
                    if (!project.beta.uploadFlavors.contains(variant.productFlavors[0].name)) {
                        project.logger.error(variantName + ":uploadFlavors does not contains " + variant.productFlavors[0].name)
                        return
                    }

                    // Create task.
                    Task betaTask = createUploadTask(variant)

                    // Check autoUpload
                    if (!project.beta.autoUpload) {
                        // dependsOn task
                        betaTask.dependsOn project.tasks["assemble${variantName}"]
                    } else {
                        // autoUpload after assemble
                        project.tasks["assemble${variantName}"].doLast {
                            // if debug model and debugOn = false no execute upload
                            if (variantName.contains("Debug") && !project.beta.debugOn) {
                                println("Bugly: the option debugOn is closed, if you want to upload apk file on debug model, you can set debugOn = true to open it")
                                return
                            }

                            if (variantName.contains("Release")) {
                                println("Bugly: the option autoUpload is opened, it will auto upload the release to the bugly platform")
                            }
                            File apkFile = variant.outputs[0].outputFile
                            uploadApk(project.beta.url, apkFile.getAbsolutePath(), project.beta.uploadParams, project.beta.resultField, project.beta.resultValue)

                        }
                    }
                }
            }
        }
    }

    /**
     * 创建上传任务
     *
     * @param variant 编译参数
     * @return
     */
    private Task createUploadTask(Object variant) {
        String variantName = variant.name.capitalize()
        Task uploadTask = project.tasks.create("upload${variantName}BetaApkFile") doLast {
            // if debug model and debugOn = false no execute upload
            if (variantName.contains("Debug") && !project.beta.debugOn) {
                println("Bugly: the option debugOn is closed, if you want to upload apk file on debug model, you can set debugOn = true to open it")
                return
            }
            File apkFile = variant.outputs[0].outputFile
            uploadApk(project.beta.url, apkFile.getAbsolutePath(), project.beta.uploadParams, project.beta.resultField, project.beta.resultValue)
        }
        println("Bugly:create upload${variantName}BetaApkFile task")
        return uploadTask
    }

    /**
     *  上传apk
     * @param uploadInfo
     * @return
     */
    public boolean uploadApk(String url, String filePath, Map<String, Object> uploadParams, String resultField, String resultValue) {
        println("apk本地路径:" + filePath)
        if (!post(url, filePath, uploadParams, resultField, resultValue)) {
            project.logger.error("Bugly: Failed to upload!")
            return false
        } else {
            println("Bugly: upload apk success !!!")
            return true
        }
    }

    /**
     * 上传apk
     * @param url 地址
     * @param filePath 文件路径
     * @param uploadInfo 更新信息
     * @return
     */
    public boolean post(String url, String filePath, Map<String, Object> uploadParams, String resultField, String resultValue) {
        HttpURLConnectionUtil connectionUtil = new HttpURLConnectionUtil(url, Constants.HTTPMETHOD_POST);
        for (String key : uploadParams.keySet()) {
            connectionUtil.addTextParameter(key, uploadParams.get(key).toString())
        }

        connectionUtil.addFileParameter(project.beta.fileField, new File(filePath));

        String result = new String(connectionUtil.post(), "UTF-8");
        def data = new JsonSlurper().parseText(result)
        if (data[resultField].toString() == resultValue) {
            println(data)
            return true
        }
        println(data)
        return false;
    }
}
