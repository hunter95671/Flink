package com.hunter95.apitest

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object ProcessFunctionTest {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val inputStream = env.socketTextStream("localhost", 7777)

    //先转换成样例类类型(简单转换操作)
    val dataStream = inputStream
      .map(data => {
        val arr = data.split(",")
        SensorReading(arr(0), arr(1).toLong, arr(2).toDouble)
      })
      .keyBy(_.id)
      .process(new MyKeyedProcessFunction)

    env.execute("process function test")
  }
}

class MyKeyedProcessFunction extends KeyedProcessFunction[String, SensorReading, String] {
  var myState:ValueState[Int]=_

  override def open(parameters: Configuration): Unit = {
    myState=getRuntimeContext.getState(new ValueStateDescriptor[Int]("mystate",classOf[Int]))
  }

  override def processElement(i: SensorReading, context: KeyedProcessFunction[String, SensorReading, String]#Context, collector: Collector[String]): Unit = {
    context.getCurrentKey
    context.timestamp()
    context.timerService().registerEventTimeTimer(context.timestamp() + 60000L)
  }

  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[String, SensorReading, String]#OnTimerContext, out: Collector[String]): Unit = {

  }
}
