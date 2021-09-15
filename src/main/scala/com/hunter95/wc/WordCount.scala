package com.hunter95.wc

import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.api.scala._

//批处理的word count
object WordCount {
  def main(args: Array[String]): Unit = {
    //创建一个批处理执行环境
    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    //从文件中取数据
    val inputPath: String = "E:\\code\\java\\FlinkTutorial\\src\\main\\resources\\hello.txt"
    val inputDataSet: DataSet[String] = env.readTextFile(inputPath)

    //对数据进行转换处理统计，先分词，再按照word进行分组，最后进行聚合统计
    val resultDataSet:DataSet[(String,Int)]=inputDataSet
      .flatMap(_.split(" "))
      .map((_,1))
      .groupBy(0)    //以第一个元素作为key进行分组
      .sum(1)         //对每组所有数据的第二个元素进行求和

    //打印输出
    resultDataSet.print()
  }
}
