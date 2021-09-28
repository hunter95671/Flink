package com.hunter95.tabletest

import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.DataTypes
import org.apache.flink.table.api.scala._
import org.apache.flink.table.descriptors.{Csv, FileSystem, Schema}

object FileOutputTest {
  def main(args: Array[String]): Unit = {
    //1.创建环境
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val tableEnv = StreamTableEnvironment.create(env)

    //2.连接外部系统，读取数据，注册表
    val filePath = "E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\sensor.txt"

    tableEnv.connect(new FileSystem().path(filePath))
      //.withFormat(new OldCsv())
      .withFormat(new Csv())
      .withSchema(new Schema()
        .field("id", DataTypes.STRING())
        .field("timestamp", DataTypes.BIGINT())
        .field("temperature", DataTypes.DOUBLE())
      )
      .createTemporaryTable("inputTable")

    //3.转换操作
    val sensorTable = tableEnv.from("inputTable")

    //3.1简单转换
    val resultTable = sensorTable
      .select('id, 'temperature)
      .filter('id === "sensor_1")

    //3.2聚合转换
    val aggTable = sensorTable
      .groupBy("id") //基于id分组
      .select('id, 'id.count as 'count)

    //4.输出到文件
    //注册输出表
    val outputPath = "E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\output.txt"

    tableEnv.connect(new FileSystem().path(outputPath))
      .withFormat(new Csv())
      .withSchema(new Schema()
        .field("id", DataTypes.STRING())
        .field("temperature", DataTypes.DOUBLE())
        //.field("cnt", DataTypes.BIGINT())  不能动态修改写入文件内容
      )
      .createTemporaryTable("outputTable")

    //resultTable.toAppendStream[(String, Double)].print("result")
    //aggTable.toRetractStream[(String, Long)].print("agg")

    resultTable.insertInto("outputTable")

    env.execute()
  }
}
