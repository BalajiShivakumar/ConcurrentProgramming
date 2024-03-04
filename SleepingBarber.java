import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SleepingBarber {
	
	public static void main (String a[]) throws InterruptedException {	
		
		int numOfBarbers=2, customerId=1, numOfCustomers=20, numOfChairs;	
		
		Scanner sc = new Scanner(System.in);
		
		System.out.println("Enter the number of Barbers[M] : ");			
    	numOfBarbers=sc.nextInt();
    	
    	System.out.println("Enter the number of waiting Chairs[N] : ");
    	numOfChairs=sc.nextInt();
    	
		ExecutorService exec = Executors.newFixedThreadPool(12);		
    	BarberShop shop = new BarberShop(numOfBarbers, numOfChairs);				
    	Random r = new Random();  										
       	    	
        System.out.println("\nNow Barber shop has "
        		+numOfBarbers+" barber(s)\n");
        
        long startTime  = System.currentTimeMillis();					
        
        for(int i=1; i<=numOfBarbers;i++) {								
        	
        	Barber barber = new Barber(shop, i);	
        	Thread thbarber = new Thread(barber);
            exec.execute(thbarber);
        }
        
        for(int i=0;i<numOfCustomers;i++) {								
        
            Customer customer = new Customer(shop);
            customer.setInTime(new Date());
            Thread thcustomer = new Thread(customer);
            customer.setcustomerId(customerId++);
            exec.execute(thcustomer);
            
            try {
            	
            	double val = r.nextGaussian() * 2000 + 2000;				
            	int millisDelay = Math.abs((int) Math.round(val));		
            	Thread.sleep(millisDelay);								
            }
            catch(InterruptedException iex) {
            
                iex.printStackTrace();
            }
            
        }
        
        exec.shutdown();												
        exec.awaitTermination(12, SECONDS);								
 
        long elapsedTime = System.currentTimeMillis() - startTime;		
        
        System.out.println("\nBarber shop is closed now");

        System.out.println("\nTotal no of customers: "+numOfCustomers+
        		"\nTotal no of customers served: "+shop.getTotalHairCutsDone()
        		+"\nTotal no of customers lost: "+shop.getCustomerLost());
               
        sc.close();
    }
}
 
class Barber implements Runnable {										

    BarberShop shop;
    int barberId;
 
    public Barber(BarberShop shop, int barberId) {
    
        this.shop = shop;
        this.barberId = barberId;
    }
    
    public void run() {
    
        while(true) {
        
            shop.cutHair(barberId);
        }
    }
}

class Customer implements Runnable {

    int customerId;
    Date inTime;
 
    BarberShop shop;
 
    public Customer(BarberShop shop) {
    
        this.shop = shop;
    }
 
    public int getCustomerId() {										
        return customerId;
    }
 
    public Date getInTime() {
        return inTime;
    }
 
    public void setcustomerId(int customerId) {
        this.customerId = customerId;
    }
 
    public void setInTime(Date inTime) {
        this.inTime = inTime;
    }
 
    public void run() {													
    
        getForHairCut();
    }
    private synchronized void getForHairCut() {							
    
        shop.add(this);
    }
}
 
class BarberShop {

	private final AtomicInteger totalHairCutsDone = new AtomicInteger(0);
	private final AtomicInteger LostCustomers = new AtomicInteger(0);
	int nchair, numOfBarbers, barbersAvailable;
    List<Customer> listOfCustomers;
    
    Random r = new Random();	 
    
    public BarberShop(int numOfBarbers, int numOfChairs){
    
        this.nchair = numOfChairs;														
        listOfCustomers = new LinkedList<Customer>();						
        this.numOfBarbers = numOfBarbers;									
        barbersAvailable = numOfBarbers;
    }
 
    public AtomicInteger getTotalHairCutsDone() {
    	
    	totalHairCutsDone.get();
    	return totalHairCutsDone;
    }
    
    public AtomicInteger getCustomerLost() {
    	
    	LostCustomers.get();
    	return LostCustomers;
    }
    
    public void cutHair(int barberId)
    {
        Customer customer;
        synchronized (listOfCustomers) {									
        															 	
            while(listOfCustomers.size()==0) {
            
                System.out.println("\nBarber "+barberId+" is waiting "
                		+ "for the customer and sleeping in his chair");
                
                try {
                
                    listOfCustomers.wait();								
                }
                catch(InterruptedException iex) {
                
                    iex.printStackTrace();
                }
            }
            
            customer = (Customer)((LinkedList<?>)listOfCustomers).poll();	
            
            System.out.println("Customer "+customer.getCustomerId()+
            		" finds the barber sleeping and wakes up "
            		+ "the barber "+barberId);
        }
        
        int millisDelay=0;
                
        try {
        	
        	barbersAvailable--; 										 
        																
            System.out.println("Barber "+barberId+" cutting hair of "+
            		customer.getCustomerId()+ " so customer sleeps");
        	
            double val = r.nextGaussian() * 2000 + 4000;				
        	millisDelay = Math.abs((int) Math.round(val));				
        	Thread.sleep(millisDelay);
        	
        	System.out.println("\nCompleted Cutting hair of "+
        			customer.getCustomerId()+" by barber " + 
        			barberId +" in "+millisDelay+ " milliseconds.");
        
        	totalHairCutsDone.incrementAndGet();
            															
            if(listOfCustomers.size()>0) {									
            	System.out.println("Barber "+barberId+					
            			" wakes up a customer from the "					
            			+ "waiting chair");		
            }
            
            barbersAvailable++;											
        }
        catch(InterruptedException iex) {
        
            iex.printStackTrace();
        }
        
    }
 
    public void add(Customer customer) {
    
        System.out.println("\nCustomer "+customer.getCustomerId()+
        		" enters through the entrance door in the the shop at "
        		+customer.getInTime());
 
        synchronized (listOfCustomers) {
        
            if(listOfCustomers.size() == nchair) {							
            
                System.out.println("\nNo chair available "
                		+ "for customer "+customer.getCustomerId()+
                		" so customer leaves the shop");
                
              LostCustomers.incrementAndGet();
                
                return;
            }
            else if (barbersAvailable > 0) {							
            															
            	((LinkedList<Customer>)listOfCustomers).offer(customer);
				listOfCustomers.notify();
			}
            else {														
            															
            	((LinkedList<Customer>)listOfCustomers).offer(customer);
                
            	System.out.println("All barber(s) are busy so "+
            			customer.getCustomerId()+
                		" takes a chair in the waiting room");
                 
                if(listOfCustomers.size()==1)
                    listOfCustomers.notify();
            }
        }
    }
}