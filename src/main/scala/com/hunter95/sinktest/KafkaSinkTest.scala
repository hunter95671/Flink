package com.hunter95.sinktest

import com.hunter95.apitest.SensorReading
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer011, FlinkKafkaProducer011}

import java.util.Properties

object KafkaSinkTest {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    //读取数据
    //    val inputPath = "E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\sensor.txt"
    //    val inputStream = env.readTextFile(inputPath)

    //从kafka读取数据
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "hadoop102:9092")
    properties.setProperty("group.id", "consumer-group")

    val stream = env.addSource(new FlinkKafkaConsumer011[String]("sensor", new SimpleStringSchema(), properties))

    //先转换成样例类类型(简单转换操作)
    val dataStream = stream
      .map(data => {
        val arr = data.split(",")
        SensorReading(arr(0), arr(1).toLong, arr(2).toDouble).toString
      })

    dataStream.addSink(
      new FlinkKafkaProducer011[String]("hadoop102:9092", "sinktest", new SimpleStringSchema()))

    env.execute("kafka sink test")
  }
}
