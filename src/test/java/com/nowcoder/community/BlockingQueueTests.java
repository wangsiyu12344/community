package com.nowcoder.community;


import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueTests {
    public static void main(String[] args) {
        BlockingQueue<Integer> blockingQueue = new ArrayBlockingQueue<>(10);
        new Thread(new Producer(blockingQueue)).start();
        new Thread(new Customer(blockingQueue)).start();
        new Thread(new Customer(blockingQueue)).start();
        new Thread(new Customer(blockingQueue)).start();
    }
}
//实现生产者线程，消费者线程
class Producer implements Runnable{

    private BlockingQueue<Integer> queue;

    public Producer(BlockingQueue<Integer> queue){
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < 100; i++) {
                Thread.sleep(20);
                queue.put(i);
                System.out.println(Thread.currentThread().getName() + "生产:" + queue.size());
            }
        }catch (InterruptedException e) {
            e.printStackTrace();
            }

    }

}
class Customer implements  Runnable{
    private BlockingQueue<Integer> queue;

    public Customer(BlockingQueue<Integer> queue){
        this.queue = queue;
    }

    @Override
    public void run() {
        try{
            while(true){
                Thread.sleep(new Random().nextInt(1000));
                queue.take();
                System.out.println(Thread.currentThread().getName() +"消费:" + queue.size());
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }
}