## Auth

### Register in Role Admin

```sh
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Admin",
    "username": "janeadmin",
    "email": "janeadmin@example.com",
    "password": "strongpassword",
    "role": ["admin"]
}'
```
### Register in Role User

```sh
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "email": "johndoe@example.com",
    "password": "securepassword"
}'
```

### Login in Role

```sh
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "realworld_user",
    "password": "realword"
}'
```

### Get Me

```sh
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer "
```

### Verify

```sh
curl -X GET "http://localhost:8080/api/auth/verify?token="
```

### Forgot Password

```sh
curl -X POST http://localhost:8080/api/auth/forgot \
  -H "Content-Type: application/json" \
  -d '{
    "email": "johndoe@example.com"
}'
```

### Reset Password

```sh
curl -X POST http://localhost:8080/api/auth/reset \
  -H "Content-Type: application/json" \
  -d '{
    "token": "<reset_token>",
    "password": "newsecurepassword"
}'
```

---

## Room

### FindAll

```sh
curl -X GET http://localhost:8080/api/room \
  -H "Authorization: Bearer <your_access_token>"
```

### Get Room By ID

```sh
curl -X GET "http://localhost:8080/api/room/1?id=1" \
  -H "Authorization: Bearer <your_access_token>"
```

### Create Room

```sh
curl -X POST http://localhost:8080/api/room/create \
  -H "Authorization: Bearer <your_access_token>" \
  -F "file=@/path/to/your/image.jpg" \
  -F "roomName=Deluxe Room" \
  -F "roomCapacity=4"
```

### Update Room

```sh
curl -X PUT http://localhost:8080/api/room/update/1?id=1 \
  -H "Authorization: Bearer <your_access_token>" \
  -F "file=@/path/to/your/image.jpg" \
  -F "roomName=Updated Room" \
  -F "roomCapacity=5"
```

### Delete Room

```sh
curl -X GET "http://localhost:8080/api/room/delete/1?id=1" \
  -H "Authorization: Bearer <your_access_token>"
```

## Booking

### FindAll

```sh
curl -X GET http://localhost:8080/api/bookings \
  -H "Authorization: Bearer <your_access_token>"
```

### Get Booking by ID

```sh
curl -X GET "http://localhost:8080/api/bookings/1?id=1" \
  -H "Authorization: Bearer <your_access_token>"
```

### Create Booking

```sh
curl -X POST http://localhost:8080/api/bookings/create \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 2,
    "totalPerson": 3,
    "bookingTime": "2025-06-10T14:00:00",
    "noted": "Request extra pillows"
  }'
```

### Update Booking

```sh
curl -X POST "http://localhost:8080/api/bookings/update/1?id=1" \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 2,
    "totalPerson": 4,
    "bookingTime": "2025-06-10T15:00:00",
    "noted": "Change time to 3PM"
  }'
```

### Delete Booking

```sh
curl -X GET "http://localhost:8080/api/bookings/delete/1?id=1" \
  -H "Authorization: Bearer <your_access_token>"
```


## Check

### Checkin

```sh
curl -X POST http://localhost:8080/api/check/check-in \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD123456",
    "checkInTime": "2025-06-05T09:00:00",
    "email": "user@example.com"
  }'
```

### Get Check Order

```sh
curl -X GET http://localhost:8080/api/check/check-order/ORD123456 \
  -H "Authorization: Bearer <your_access_token>"
```

### Check out

```sh
curl -X POST http://localhost:8080/api/check/check-out/ORD123456 \
  -H "Authorization: Bearer <your_access_token>"
```