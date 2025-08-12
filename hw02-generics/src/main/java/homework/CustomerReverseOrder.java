package homework;

import java.util.LinkedList;
import java.util.List;

public class CustomerReverseOrder {

    private List<Customer> customers;

    public void add(Customer customer) {
        if (customers == null) {
            customers = new LinkedList<>();
        }
        customers.add(customer);
    }

    public Customer take() {
        return customers.removeLast();
    }
}
