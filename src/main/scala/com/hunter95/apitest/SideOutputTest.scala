package com.hunter95.apitest

import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object SideOutputTest {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    //设置状态后端
    env.setStateBackend(new FsStateBackend(""))

    val inputStream = env.socketTextStream("localhost", 7777)

    //先转换成样例类类型(简单转换操作)
    val dataStream = inputStream
      .map(data => {
        val arr = data.split(",")
        SensorReading(arr(0), arr(1).toLong, arr(2).toDouble)
      })

    val highTempStream=dataStream
      .process(new SplitTempProcessor(30.0))

    highTempStream.print("high")

    highTempStream.getSideOutput(new OutputTag[(String,Long,Double)]("low")).print("low")

    env.execute("side output test")
  }
}

//实现自定义的ProcessFunction，进行分流
class SplitTempProcessor(threshold: Double) extends ProcessFunction[SensorReading,SensorReading]{
  override def processElement(i: SensorReading, context: ProcessFunction[SensorReading, SensorReading]#Context, collector: Collector[SensorReading]): Unit = {
    if(i.temperature>threshold){
      //如果当前温度大于30，那么输出到主流
      collector.collect(i)
    }else{
      //如果不超过30度，那么输出到侧输出流
      context.output(new OutputTag[(String,Long,Double)]("low"),(i.id,i.timestamp,i.temperature))
    }
  }
}