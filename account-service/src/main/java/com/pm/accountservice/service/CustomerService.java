package com.pm.accountservice.service;

import com.pm.accountservice.dto.CreateCustomerRequestDTO;
import com.pm.accountservice.dto.CustomerResponseDTO;
import com.pm.accountservice.exception.DuplicateResourceException;
import com.pm.accountservice.model.Customer;
import com.pm.accountservice.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponseDTO createCustomer(CreateCustomerRequestDTO createCustomerRequestDTO) {
        String email = createCustomerRequestDTO.email().trim().toLowerCase(Locale.ROOT);

        if(customerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Customer with this email already exists");
        }

        if(customerRepository.existsByNationalId(createCustomerRequestDTO.nationalId())) {
            throw new DuplicateResourceException("Customer with this National Id already exists");
        }

        Customer customer = new Customer();
        customer.setName(createCustomerRequestDTO.name());
        customer.setEmail(email);
        customer.setNationalId(createCustomerRequestDTO.nationalId());

        Customer newCustomer = customerRepository.saveAndFlush(customer);

        return toResponse(newCustomer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();

        List<CustomerResponseDTO> customerResponseDTOS = new ArrayList<>();
        for (Customer customer : customers) {
            customerResponseDTOS.add(toResponse(customer));
        }
        return customerResponseDTOS;
    }

    private CustomerResponseDTO toResponse(Customer customer) {
        return new CustomerResponseDTO(customer.getId(), customer.getName(), customer.getEmail());
    }
}
