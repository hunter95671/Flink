package com.hunter95.apitest

import org.apache.flink.api.common.functions.{ReduceFunction, RichMapFunction}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._

object TransformTest {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    //0.读取数据
    val inputPath = "E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\sensor.txt"
    val inputStream = env.readTextFile(inputPath)

    //1.先转换成样例类类型(简单转换操作)
    val dataStream = inputStream
      .map(data => {
        val arr = data.split(",")
        SensorReading(arr(0), arr(1).toLong, arr(2).toDouble)
      })

    //2.分组聚合，输出每个传感器当前最小值
    val aggStream = dataStream
      .keyBy("id") //根据id进行分组
      .minBy("temperature")

    //aggStream.print()

    //3.需要输出当前最小的温度值，已经最近的时间戳，要用reduce
    val resultStream = dataStream
      .keyBy("id")
      .reduce((curState, newData) =>
        SensorReading(curState.id, newData.timestamp, curState.temperature.min(newData.temperature))
      )

    //    val resultStream = dataStream
    //      .keyBy("id")
    //      .reduce(new MyReduceFunction)

    //    resultStream.print()

    //4.多流转换
    //4.1 分流，将传感器温度分成低温、高温两条流
    val splitStream = dataStream
      .split(data => {
        if (data.temperature > 30.0) Seq("high") else Seq("low")
      })

    val highTempStream = splitStream.select("high")
    val lowTempStream = splitStream.select("low")
    val allTempStream = splitStream.select("high", "low")

    //    highTempStream.print("high")
    //    lowTempStream.print("low")
    //    allTempStream.print("all")

    //4.2 合流操作，connect
    val warningStream = highTempStream.map(data => (data.id, data.temperature))
    val connectedStreams = warningStream.connect(lowTempStream)

    //用coMap对数据进行分别处理
    val coMapResultStream = connectedStreams
      .map(
        warningData => (warningData._1, warningData._2, "warning"),
        lowTempData => (lowTempData.id, "healthy")
      )

    coMapResultStream.print()

    //4.3 union合流(类型必须一样，可以一次连接多个)
    val unionStream = highTempStream.union(lowTempStream, allTempStream)

    unionStream.print()

    env.execute("transform test")
  }
}

class MyReduceFunction extends ReduceFunction[SensorReading] {
  override def reduce(t: SensorReading, t1: SensorReading): SensorReading = {
    SensorReading(t.id, t1.timestamp, t.temperature.min(t1.temperature))
  }
}

//富函数，可以获取到运行时上下文，还有一些生命周期
class MyRichMapper extends RichMapFunction[SensorReading, String] {

  override def open(parameters: Configuration): Unit = {
    //做一些初始化操作，比如数据库的连接
    //getRuntimeContext.getState()
  }

  override def map(in: SensorReading): String = in.id + "temperature"

  override def close(): Unit = {
    //一般做守卫工作，比如关闭连接，或者清空状态

  }
}
