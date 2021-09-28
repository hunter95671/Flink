package com.hunter95.tabletest

import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.scala._

object KafkaPipelineTest {
  def main(args: Array[String]): Unit = {
    //1.创建环境
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val tableEnv = StreamTableEnvironment.create(env)
  }
}
