package com.hunter95.udftest

import com.hunter95.apitest.SensorReading
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala._
import org.apache.flink.table.functions.AggregateFunction
import org.apache.flink.types.Row

object AggregateFunctionTest {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val settings = EnvironmentSettings.newInstance()
      .useBlinkPlanner()
      .inStreamingMode()
      .build()
    val tableEnv = StreamTableEnvironment.create(env, settings)

    val inputPath = "E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\sensor.txt"
    val inputStream = env.readTextFile(inputPath)

    //先转换成样例类类型(简单转换操作)
    val dataStream = inputStream
      .map(data => {
        val arr = data.split(",")
        SensorReading(arr(0), arr(1).toLong, arr(2).toDouble)
      })
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(1)) {
        override def extractTimestamp(t: SensorReading): Long = t.timestamp * 1000L
      })
    val sensorTable = tableEnv.fromDataStream(dataStream, 'id, 'temperature, 'timestamp, 'timestamp.rowtime as 'ts)

    //table api
    val avgTemp = new AvgTemp
    val resultTable = sensorTable
      .groupBy('id)
      .aggregate(avgTemp('temperature) as 'avgTemp)
      .select('id, 'avgTemp)

    //sql
    tableEnv.createTemporaryView("sensor", sensorTable)
    tableEnv.registerFunction("avgTemp", avgTemp)
    val resultSqlTable = tableEnv.sqlQuery(
      """
        |select id,avgTemp(temperature)
        |from
        |sensor
        |group by id
        |""".stripMargin
    )

    resultTable.toRetractStream[Row].print("result")
    resultSqlTable.toRetractStream[Row].print("sql")

    env.execute("aggregate function test")
  }
}

//定义一个类用于表示聚合的状态
class AvgTempAcc {
  var sum: Double = 0.0
  var count: Int = 0
}

//自定义一个聚合函数，求每个传感器的平均温度值，保存状态(tempSum,tempCount)
class AvgTemp extends AggregateFunction[Double, AvgTempAcc] {
  override def getValue(acc: AvgTempAcc): Double = acc.sum / acc.count

  override def createAccumulator(): AvgTempAcc = new AvgTempAcc

  //还要实现结果具体的处理计算函数,accumulate
  def accumulate(accumulator: AvgTempAcc, temp: Double): Unit = {
    accumulator.sum += temp
    accumulator.count += 1

  }
}
