package com.hunter95.wc

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._

//流处理word count
object StreamWordCount {
  def main(args: Array[String]): Unit = {
    //创建流处理执行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(4)

    //从外部命令中提取参数，作为socket主机名和端口号
    val paramTool:ParameterTool=ParameterTool.fromArgs(args)
    val host:String=paramTool.get("host")
    val port:Int=paramTool.getInt("port")
    //接收一个socket文本流
    val inputDataStream: DataStream[String] = env.socketTextStream(host,port)

    //进行转换处理统计
    val resultDataStream: DataStream[(String, Int)] = inputDataStream
      .flatMap(_.split(" "))
      .filter(_.nonEmpty)
      .map((_, 1))
      .keyBy(0) //按照key值分组
      .sum(1)

    //打印输出
    resultDataStream.print().setParallelism(1)

    //启动任务执行
    env.execute("stream word count")
  }
}
