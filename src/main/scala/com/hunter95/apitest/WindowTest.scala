package com.hunter95.apitest

import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.{EventTimeSessionWindows, SlidingProcessingTimeWindows, TumblingEventTimeWindows}
import org.apache.flink.streaming.api.windowing.time.Time

object WindowTest {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    //设置时间语义
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.getConfig.setAutoWatermarkInterval(500)

    //读取数据
    //val inputPath = "E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\sensor.txt"
    //val inputStream = env.readTextFile(inputPath)

    val inputStream = env.socketTextStream("localhost", 7777)

    //先转换成样例类类型(简单转换操作)
    val dataStream = inputStream
      .map(data => {
        val arr = data.split(",")
        SensorReading(arr(0), arr(1).toLong, arr(2).toDouble)
      })
      //.assignAscendingTimestamps(_.timestamp * 1000L) //升序数据提取时间戳
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(3)) {
        override def extractTimestamp(t: SensorReading): Long = t.timestamp * 1000L
      })

    val latetag=new OutputTag[(String, Double, Long)]("late")
    //每15秒统计一次，窗口内各传感器所有温度的最小值
    val resultStream = dataStream
      .map(data => (data.id, data.temperature, data.timestamp))
      .keyBy(_._1)
      //.window(TumblingEventTimeWindows.of(Time.seconds(15)))  //滚动时间窗口
      //.window(SlidingProcessingTimeWindows.of(Time.seconds(15),Time.seconds(3)))   //滑动时间窗口
      //.window(EventTimeSessionWindows.withGap(Time.seconds(10)))   //回话窗口
      //.countWindow(10)   //滚动计数窗口
      .timeWindow(Time.seconds(15)) //一个参数为滚动时间窗口，两个参数为滑动时间窗口
      .allowedLateness(Time.seconds(1))    //允许接收迟到数据
      .sideOutputLateData(latetag)  //侧输出流
      //.minBy(1)
      .reduce((curRes, newData) => (curRes._1, curRes._2.min(newData._2), newData._3))

    resultStream.getSideOutput(latetag).print("late")
    resultStream.print("result")

    env.execute("window test")
  }
}

//也可以自定义reduce函数
class MyReducer extends ReduceFunction[SensorReading] {
  override def reduce(t: SensorReading, t1: SensorReading): SensorReading = {
    SensorReading(t1.id, t1.timestamp, t.temperature.min(t1.temperature))
  }
}
