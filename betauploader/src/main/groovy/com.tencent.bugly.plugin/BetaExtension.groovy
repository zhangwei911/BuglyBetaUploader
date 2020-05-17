package com.tencent.bugly.plugin

/**
 * Beta extension params
 * @author wenjiewu
 */
public class BetaExtension {
    public String url = null // 指定上传的url
    public String resultField = null // 指定上传成功结果字段
    public String resultValue = null // 指定上传成功字段值
    public String apkFile = null // 指定上传的apk文件
    public Boolean enable = true // 插件开关
    public Boolean autoUpload = false // 是否自动上传
    public Boolean debugOn = false // debug模式是否上传
    public Map<String,Object> uploadParams = new HashMap<>()
    public List<String> uploadFlavors = new ArrayList<>()
    public String fileField = null
}