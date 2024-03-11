package vn.edu.iuh.fit.springboot_redis.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import vn.edu.iuh.fit.springboot_redis.models.User;
import vn.edu.iuh.fit.springboot_redis.repositories.UserRepository;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@RestController
public class UserController {
    @Autowired
    private UserRepository userRepository;
    private Jedis jedis = new Jedis();

    @GetMapping("/users")
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/user/{id}")
    public User findUserById(@PathVariable("id") long id) throws ParseException {
        String key = String.valueOf(id);

        //Kiểm tra trong cache có user này không
        if(jedis.exists(key)){
            User userInCache = new User();
            userInCache.setId(id);
            userInCache.setName(jedis.hget(key, "name"));
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            userInCache.setDob(formatter.parse(jedis.hget(key, "dob")));
            return userInCache;
        }
        else{
            User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

            //Cache vào redis
            jedis.hset(key, "name", user.getName());
            jedis.hset(key, "dob", user.getDob().toString());
            return user;
        }
    }

    @PutMapping("/user/{id}")
    public User updateUser(@PathVariable("id") long id, @RequestBody User user) {
        System.out.println(user);
        User currentUser = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        currentUser.setName(user.getName());
        currentUser.setDob(user.getDob());
        jedis.hset(String.valueOf(id), "name", user.getName());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        jedis.hset(String.valueOf(id), "dob", dateFormat.format(user.getDob()));
        return userRepository.save(currentUser);
    }

    @DeleteMapping("/user/{id}")
    public void deleteUser(@PathVariable("id") long id){
        User currentUser = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(currentUser);
        jedis.del(String.valueOf(currentUser.getId()));
    }
}
