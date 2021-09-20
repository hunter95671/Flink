package com.hunter95.apitest.sinktest

import com.hunter95.apitest.SensorReading
import org.apache.flink.api.common.serialization.SimpleStringEncoder
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink
import org.apache.flink.streaming.api.scala._

object FileSink {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    //读取数据
    val inputPath = "E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\sensor.txt"
    val inputStream = env.readTextFile(inputPath)

    //先转换成样例类类型(简单转换操作)
    val dataStream = inputStream
      .map(data => {
        val arr = data.split(",")
        SensorReading(arr(0), arr(1).toLong, arr(2).toDouble)
      })

    dataStream.print()
    //dataStream.writeAsCsv("E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\output.txt")
    dataStream.addSink(StreamingFileSink.forRowFormat(
      new Path("E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\output.txt"),
      new SimpleStringEncoder[SensorReading]()
    ).build()
    )
    env.execute("file sink test")
  }
}
