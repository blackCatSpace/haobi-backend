# 项目介绍

***

基于Springboot + RabbitMQ + AIGC的智能数据分析平台。区别于传统BI，用户只需要导入原始数据 集、并输入分析诉求，就能自动生成可视化图表和分析结论，实现数据分析降本增效。

# 效果展示

***
[查看在线展示](https://blackcat.icu/)

# 功能介绍

***
- 后端自定义Prompt预设模板并封装用户输入的数据和分析诉求，通过对接AIGC接口生成可视化json配置分析结论，返回给前端解析渲染。 
- 由于AIGC的输入的限制，使用Easy Excel解析用户上传的XLSX表格数据文件并压缩为CSV，压缩了单次输入 量。 为保证文件上传的安全性，需要对用户上传的原始数据文件进行后缀名、大小、内容等多重校验。 
- 为防止恶意用户占用系统资源，基于Redisson的RateLimeter并采用令牌桶算法实现分布式限流，控制单个用户 访问AIGC服务的频率。
- 由于AIGC的响应时间较长，基于自定义IO密集型线程池 + MQ实现异步的调用AIGC接口，支持更多用户排队同 时提高用户体验，而不是无限给系统压力导致提交失败。
