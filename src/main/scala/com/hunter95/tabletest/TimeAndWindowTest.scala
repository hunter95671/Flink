package com.hunter95.tabletest

import com.hunter95.apitest.SensorReading
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala._
import org.apache.flink.types.Row

object TimeAndWindowTest {
  def main(args: Array[String]): Unit = {
    val env=StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)

    val settings=EnvironmentSettings.newInstance()
      .useBlinkPlanner()
      .inStreamingMode()
      .build()
    val tableEnv=StreamTableEnvironment.create(env,settings)

    val inputPath = "E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\sensor.txt"
    val inputStream = env.readTextFile(inputPath)

    //先转换成样例类类型(简单转换操作)
    val dataStream = inputStream
      .map(data => {
        val arr = data.split(",")
        SensorReading(arr(0), arr(1).toLong, arr(2).toDouble)
      })

    val sensorTable=tableEnv.fromDataStream(dataStream,'id,'temperature,'timestamp,'pt.proctime)

    sensorTable.printSchema()
    sensorTable.toAppendStream[Row].print()

    env.execute("time and window test")
  }
}
