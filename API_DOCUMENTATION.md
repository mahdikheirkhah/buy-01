# 📚 Buy-01 API Documentation

Complete API reference for the Buy-01 E-Commerce Platform.

**Base URL**: `https://localhost:8443`

**Authentication**: JWT tokens stored in HTTP-only secure cookies (automatically set after login)

---

## Table of Contents

- [Authentication](#authentication)
  - [Register](#register-user)
  - [Login](#login)
  - [Logout](#logout)
- [Users](#users)
  - [Get Current User](#get-current-user)
  - [Get User by ID](#get-user-by-id)
  - [Update User Profile](#update-user-profile)
  - [Delete User](#delete-user)
  - [Update Avatar](#update-avatar)
  - [Delete Avatar](#delete-avatar)
- [Seller Profiles](#seller-profiles)
  - [Get Authenticated Seller Profile](#get-authenticated-seller-profile)
  - [Get Public Seller Profile](#get-public-seller-profile)
  - [Get Seller Statistics](#get-seller-statistics)
  - [Update Seller Profile](#update-seller-profile)
- [Products](#products)
  - [Get All Products](#get-all-products)
  - [Get My Products](#get-my-products)
  - [Get Product by ID](#get-product-by-id)
  - [Search Products](#search-products)
  - [Create Product](#create-product)
  - [Update Product](#update-product)
  - [Delete Product](#delete-product)
  - [Upload Product Image](#upload-product-image)
  - [Delete Product Image](#delete-product-image)
- [Orders](#orders)
  - [Create Order](#create-order)
  - [Get Order by ID](#get-order-by-id)
  - [Get User Orders](#get-user-orders)
  - [Get Active Cart](#get-active-cart)
  - [Checkout Order](#checkout-order)
  - [Update Order Status](#update-order-status)
  - [Cancel Order](#cancel-order)
  - [Redo Order](#redo-order)
  - [Add Item to Order](#add-item-to-order)
  - [Update Order Item](#update-order-item)
  - [Remove Item from Order](#remove-item-from-order)
  - [Clear Cart Items](#clear-cart-items)
  - [Get User Stats](#get-user-stats)
  - [Get Seller Stats](#get-seller-stats)
- [Media](#media)
  - [Upload File](#upload-file)
  - [Upload Avatar](#upload-avatar)
  - [Get File](#get-file)
  - [Get Product Image URLs](#get-product-image-urls)
  - [Get Media Batch](#get-media-batch)
  - [Delete Media](#delete-media)
- [Data Models](#data-models)

---

## Authentication

### Register User

Creates a new user account.

**Endpoint**: `POST /api/auth/register`

**Content-Type**: `multipart/form-data`

**Request Body**:

| Part         | Type | Required | Description            |
| ------------ | ---- | -------- | ---------------------- |
| `userDto`    | JSON | Yes      | User registration data |
| `avatarFile` | File | No       | User avatar image      |

**userDto Schema**:

```json
{
  "firstName": "string", // Required, min 1 character
  "lastName": "string", // Required, min 1 character
  "email": "string", // Required, valid email format
  "password": "string", // Required, min 5 characters
  "role": "CLIENT | SELLER" // Optional, defaults to CLIENT
}
```

**Response** `200 OK`:

```json
{
  "message": "User registered successfully"
}
```

**Error Responses**:

| Status | Description                                            |
| ------ | ------------------------------------------------------ |
| `400`  | Validation error (invalid email, short password, etc.) |
| `409`  | Email already exists                                   |

---

### Login

Authenticates a user and sets JWT cookie.

**Endpoint**: `POST /api/auth/login`

**Content-Type**: `application/json`

**Request Body**:

```json
{
  "email": "string", // Required, valid email
  "password": "string" // Required, min 5 characters
}
```

**Response** `200 OK`:

```json
{
  "message": "Login successful"
}
```

**Note**: On success, an HTTP-only secure cookie `jwt` is automatically set:

```
Set-Cookie: jwt=eyJhbGc...; HttpOnly; Secure; SameSite=Strict; Path=/
```

**Error Responses**:

| Status | Description               |
| ------ | ------------------------- |
| `401`  | Invalid email or password |

---

### Logout

Logs out the current user by clearing the JWT cookie.

**Endpoint**: `POST /api/users/logout`

**Authentication**: Required (Cookie)

**Response** `200 OK`:

```json
{
  "message": "Logout successful"
}
```

---

## Users

### Get Current User

Returns the authenticated user's profile.

**Endpoint**: `GET /api/users/me`

**Authentication**: Required (Cookie)

**Response** `200 OK` - [InfoUserDTO](#infouserdto):

```json
{
  "id": "507f1f77bcf86cd799439011",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "avatarUrl": "/api/media/files/avatar123.jpg",
  "role": "CLIENT"
}
```

**Error Responses**:

| Status | Description       |
| ------ | ----------------- |
| `401`  | Not authenticated |

---

### Get User by ID

Returns a user's public profile by ID.

**Endpoint**: `GET /api/users/seller`

**Query Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `id`      | string | Yes      | User ID     |

**Response** `200 OK` - [InfoUserDTO](#infouserdto):

```json
{
  "id": "507f1f77bcf86cd799439011",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "avatarUrl": "/api/media/files/avatar123.jpg",
  "role": "SELLER"
}
```

---

### Update User Profile

Updates the authenticated user's profile.

**Endpoint**: `PUT /api/users/me`

**Authentication**: Required (Cookie)

**Content-Type**: `application/json`

**Request Body** - [UpdateUserDTO](#updateuserdto):

```json
{
  "email": "newemail@example.com", // Optional, valid email
  "firstName": "John", // Optional
  "lastName": "Doe", // Optional
  "currentPassword": "oldPassword123", // Required for password change
  "newPassword": "newPassword456" // Optional, min 5 characters
}
```

**Response** `200 OK`:

```json
{
  "message": "updated successfully"
}
```

**Note**: If email is changed, a new JWT cookie is automatically set.

---

### Delete User

Deletes the authenticated user's account.

**Endpoint**: `DELETE /api/users`

**Authentication**: Required (Cookie)

**Query Parameters**:

| Parameter  | Type   | Required | Description                              |
| ---------- | ------ | -------- | ---------------------------------------- |
| `password` | string | Yes      | User's current password for verification |

**Response** `200 OK`:

```json
{
  "message": "user deleted successfully"
}
```

---

### Update Avatar

Updates the authenticated user's avatar image.

**Endpoint**: `POST /api/users/newAvatar`

**Authentication**: Required (Cookie, SELLER or ADMIN role)

**Content-Type**: `multipart/form-data`

**Request Body**:

| Part         | Type | Required | Description           |
| ------------ | ---- | -------- | --------------------- |
| `avatarFile` | File | Yes      | New avatar image file |

**Response** `200 OK`:

```json
{
  "message": "Avatar updated successfully"
}
```

---

### Delete Avatar

Deletes the authenticated user's avatar.

**Endpoint**: `DELETE /api/users/avatar`

**Authentication**: Required (Cookie, SELLER or ADMIN role)

**Response** `200 OK`:

```
avatar deleted successfully
```

---

## Seller Profiles

### Get Authenticated Seller Profile

Returns the authenticated seller's complete profile.

**Endpoint**: `GET /api/sellers/profile`

**Authentication**: Required (Cookie, SELLER role)

**Response** `200 OK` - [SellerProfileDTO](#sellerprofiledto):

```json
{
  "sellerId": "507f1f77bcf86cd799439011",
  "sellerName": "John's Electronics",
  "shopLogoUrl": "/api/media/files/logo.jpg",
  "shopDescription": "Quality electronics at great prices",
  "totalRevenue": 15000.5,
  "totalSales": 150,
  "totalOrders": 120,
  "totalCustomers": 85,
  "bestSellingProductId": "507f1f77bcf86cd799439012",
  "bestSellingProductName": "Wireless Headphones",
  "bestSellingProductCount": 45,
  "averageRating": 4.7,
  "totalReviews": 89,
  "totalFiveStarReviews": 62,
  "isVerified": true,
  "isActive": true,
  "deliveryRating": 4.8,
  "communicationRating": 4.6,
  "returnRate": 2,
  "cancellationRate": 1,
  "joinDate": "2024-01-15T10:30:00Z",
  "lastSaleDate": "2025-02-05T14:22:00Z",
  "categories": ["Electronics", "Audio"],
  "followerCount": 234
}
```

**Error Responses**:

| Status | Description          |
| ------ | -------------------- |
| `401`  | Not authenticated    |
| `403`  | User is not a seller |

---

### Get Public Seller Profile

Returns a seller's public profile by ID.

**Endpoint**: `GET /api/sellers/{sellerId}/profile`

**Path Parameters**:

| Parameter  | Type   | Required | Description      |
| ---------- | ------ | -------- | ---------------- |
| `sellerId` | string | Yes      | Seller's user ID |

**Response** `200 OK` - [SellerProfileDTO](#sellerprofiledto)

---

### Get Seller Statistics

Returns a seller's business statistics.

**Endpoint**: `GET /api/sellers/{sellerId}/statistics`

**Path Parameters**:

| Parameter  | Type   | Required | Description      |
| ---------- | ------ | -------- | ---------------- |
| `sellerId` | string | Yes      | Seller's user ID |

**Response** `200 OK` - [SellerProfileDTO](#sellerprofiledto)

---

### Update Seller Profile

Updates the authenticated seller's profile.

**Endpoint**: `PUT /api/sellers/profile`

**Authentication**: Required (Cookie, SELLER role)

**Content-Type**: `application/json`

**Request Body** - Partial [SellerProfileDTO](#sellerprofiledto):

```json
{
  "shopDescription": "Updated shop description",
  "categories": ["Electronics", "Audio", "Accessories"]
}
```

**Response** `200 OK` - [SellerProfileDTO](#sellerprofiledto)

---

## Products

### Get All Products

Returns a paginated list of all products.

**Endpoint**: `GET /api/products/all`

**Query Parameters**:

| Parameter | Type   | Default          | Description              |
| --------- | ------ | ---------------- | ------------------------ |
| `page`    | int    | 0                | Page number (0-indexed)  |
| `size`    | int    | 10               | Items per page           |
| `sort`    | string | `createdAt,desc` | Sort field and direction |

**Response** `200 OK` - [Page](#paget)\<[ProductCardDTO](#productcarddto)\>:

```json
{
  "content": [
    {
      "id": "507f1f77bcf86cd799439011",
      "name": "Wireless Headphones",
      "description": "High-quality wireless headphones with noise cancellation",
      "price": 149.99,
      "quantity": 50,
      "createdByMe": false,
      "imageUrls": ["/api/media/files/img1.jpg", "/api/media/files/img2.jpg"]
    }
  ],
  "totalElements": 156,
  "totalPages": 16,
  "number": 0
}
```

---

### Get My Products

Returns products created by the authenticated seller.

**Endpoint**: `GET /api/products/my-products`

**Authentication**: Required (Cookie, SELLER or ADMIN role)

**Query Parameters**:

| Parameter | Type   | Default          | Description              |
| --------- | ------ | ---------------- | ------------------------ |
| `page`    | int    | 0                | Page number (0-indexed)  |
| `size`    | int    | 10               | Items per page           |
| `sort`    | string | `createdAt,desc` | Sort field and direction |

**Response** `200 OK` - [Page](#paget)\<[ProductCardDTO](#productcarddto)\>

---

### Get Product by ID

Returns detailed product information.

**Endpoint**: `GET /api/products/{productId}`

**Authentication**: Required (Cookie)

**Path Parameters**:

| Parameter   | Type   | Required | Description |
| ----------- | ------ | -------- | ----------- |
| `productId` | string | Yes      | Product ID  |

**Response** `200 OK` - [ProductDTO](#productdto):

```json
{
  "productId": "507f1f77bcf86cd799439011",
  "name": "Wireless Headphones",
  "description": "High-quality wireless headphones with noise cancellation",
  "price": 149.99,
  "quantity": 50,
  "sellerFirstName": "John",
  "sellerLastName": "Doe",
  "sellerEmail": "john.doe@example.com",
  "createdByMe": true,
  "media": [
    {
      "fileId": "507f1f77bcf86cd799439012",
      "fileUrl": "/api/media/files/img1.jpg"
    }
  ]
}
```

---

### Search Products

Search and filter products with optional criteria.

**Endpoint**: `GET /api/products/search`

**Query Parameters**:

| Parameter     | Type   | Required | Description                               |
| ------------- | ------ | -------- | ----------------------------------------- |
| `q`           | string | No       | Keyword search (name & description)       |
| `minPrice`    | number | No       | Minimum price                             |
| `maxPrice`    | number | No       | Maximum price                             |
| `minQuantity` | int    | No       | Minimum quantity available                |
| `maxQuantity` | int    | No       | Maximum quantity available                |
| `startDate`   | string | No       | Products created after (ISO 8601)         |
| `endDate`     | string | No       | Products created before (ISO 8601)        |
| `page`        | int    | No       | Page number (default: 0)                  |
| `size`        | int    | No       | Items per page (default: 20)              |
| `sort`        | string | No       | Sort criteria (default: `createdAt,desc`) |

**Examples**:

- All products: `GET /api/products/search`
- Search by keyword: `GET /api/products/search?q=laptop`
- Filter by price: `GET /api/products/search?minPrice=500&maxPrice=1500`
- Combined: `GET /api/products/search?q=laptop&minPrice=500&maxPrice=1500`

**Response** `200 OK` - [Page](#paget)\<[ProductCardDTO](#productcarddto)\>

---

### Create Product

Creates a new product.

**Endpoint**: `POST /api/products`

**Authentication**: Required (Cookie, SELLER or ADMIN role)

**Content-Type**: `application/json`

**Request Body** - [CreateProductDTO](#createproductdto):

```json
{
  "name": "Wireless Headphones", // Required, 3-100 chars
  "description": "High-quality...", // Required, min 5 chars
  "price": 149.99, // Required, positive number
  "quantity": 50 // Required, zero or positive
}
```

**Response** `201 Created` - [Product](#product):

```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "Wireless Headphones",
  "description": "High-quality wireless headphones with noise cancellation",
  "price": 149.99,
  "quantity": 50,
  "sellerID": "507f1f77bcf86cd799439012",
  "createdAt": "2025-02-06T10:30:00Z",
  "updatedAt": "2025-02-06T10:30:00Z"
}
```

---

### Update Product

Updates an existing product.

**Endpoint**: `PUT /api/products/{productId}`

**Authentication**: Required (Cookie, SELLER or ADMIN role, owner only)

**Path Parameters**:

| Parameter   | Type   | Required | Description |
| ----------- | ------ | -------- | ----------- |
| `productId` | string | Yes      | Product ID  |

**Request Body** - [UpdateProductDTO](#updateproductdto):

```json
{
  "name": "Updated Name", // Optional, 3-100 chars
  "description": "Updated...", // Optional, min 5 chars
  "price": 129.99, // Optional, zero or positive
  "quantity": 75 // Optional, non-negative
}
```

**Response** `200 OK` - [UpdateProductDTO](#updateproductdto)

---

### Delete Product

Deletes a product.

**Endpoint**: `DELETE /api/products/{productId}`

**Authentication**: Required (Cookie, SELLER or ADMIN role, owner only)

**Path Parameters**:

| Parameter   | Type   | Required | Description |
| ----------- | ------ | -------- | ----------- |
| `productId` | string | Yes      | Product ID  |

**Response** `200 OK`:

```
Product deleted successfully
```

---

### Upload Product Image

Uploads an image for a product.

**Endpoint**: `POST /api/products/create/images`

**Authentication**: Required (Cookie, SELLER or ADMIN role)

**Content-Type**: `multipart/form-data`

**Request Body**:

| Parameter   | Type   | Required | Description |
| ----------- | ------ | -------- | ----------- |
| `productId` | string | Yes      | Product ID  |
| `file`      | File   | Yes      | Image file  |

**Response** `200 OK`:

```json
{
  "message": "Image created successfully"
}
```

---

### Delete Product Image

Deletes an image from a product.

**Endpoint**: `DELETE /api/products/deleteMedia/{productId}/{mediaId}`

**Authentication**: Required (Cookie, SELLER or ADMIN role, owner only)

**Path Parameters**:

| Parameter   | Type   | Required | Description        |
| ----------- | ------ | -------- | ------------------ |
| `productId` | string | Yes      | Product ID         |
| `mediaId`   | string | Yes      | Media ID to delete |

**Response** `200 OK`:

```json
{
  "message": "Media deleted successfully"
}
```

---

## Orders

### Create Order

Creates a new order (shopping cart).

**Endpoint**: `POST /api/orders`

**Authentication**: Required (Cookie)

**Content-Type**: `application/json`

**Request Body** - [CreateOrderRequest](#createorderrequest):

```json
{
  "userId": "507f1f77bcf86cd799439011",
  "shippingAddress": "123 Main St, City, Country",
  "items": [
    {
      "productId": "507f1f77bcf86cd799439012",
      "quantity": 2
    }
  ],
  "paymentMethod": "CARD"
}
```

**Response** `200 OK` - [Order](#order):

```json
{
  "id": "507f1f77bcf86cd799439013",
  "userId": "507f1f77bcf86cd799439011",
  "shippingAddress": "123 Main St, City, Country",
  "status": "PENDING",
  "items": [
    {
      "productId": "507f1f77bcf86cd799439012",
      "quantity": 2,
      "price": 149.99,
      "sellerId": "507f1f77bcf86cd799439014",
      "productName": "Wireless Headphones"
    }
  ],
  "paymentMethod": "CARD",
  "orderDate": null,
  "createdAt": "2025-02-06T10:30:00Z",
  "updatedAt": "2025-02-06T10:30:00Z"
}
```

---

### Get Order by ID

Returns a specific order.

**Endpoint**: `GET /api/orders/{orderId}`

**Authentication**: Required (Cookie, owner or ADMIN)

**Path Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `orderId` | string | Yes      | Order ID    |

**Response** `200 OK` - [Order](#order)

---

### Get User Orders

Returns paginated orders for a user.

**Endpoint**: `GET /api/orders/user/{userId}`

**Authentication**: Required (Cookie, same user or ADMIN)

**Path Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `userId`  | string | Yes      | User ID     |

**Query Parameters**:

| Parameter | Type | Default | Description    |
| --------- | ---- | ------- | -------------- |
| `page`    | int  | 0       | Page number    |
| `size`    | int  | 10      | Items per page |

**Response** `200 OK` - [Page](#paget)\<[Order](#order)\>

---

### Get Active Cart

Returns the user's current pending order (cart).

**Endpoint**: `GET /api/orders/user/{userId}/cart`

**Authentication**: Required (Cookie, same user or ADMIN)

**Path Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `userId`  | string | Yes      | User ID     |

**Response** `200 OK` - [Order](#order)

**Response** `204 No Content` - No active cart exists

---

### Checkout Order

Finalizes and processes an order.

**Endpoint**: `POST /api/orders/{orderId}/checkout`

**Authentication**: Required (Cookie, owner only)

**Path Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `orderId` | string | Yes      | Order ID    |

**Request Body** - [CheckoutRequest](#checkoutrequest):

```json
{
  "shippingAddress": "123 Main St, City, Country",
  "paymentMethod": "CARD"
}
```

**Response** `200 OK` - [Order](#order) (with status changed to `PROCESSING`)

---

### Update Order Status

Updates an order's status (Admin only).

**Endpoint**: `PUT /api/orders/{orderId}/status`

**Authentication**: Required (Cookie, ADMIN role)

**Path Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `orderId` | string | Yes      | Order ID    |

**Request Body** - [UpdateOrderStatusRequest](#updateorderstatusrequest):

```json
{
  "status": "SHIPPED"
}
```

**Response** `200 OK` - [Order](#order)

---

### Cancel Order

Cancels an order and restocks items.

**Endpoint**: `DELETE /api/orders/{orderId}`

**Authentication**: Required (Cookie, owner or ADMIN)

**Path Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `orderId` | string | Yes      | Order ID    |

**Response** `204 No Content`

---

### Redo Order

Creates a new order with the same items as a previous order.

**Endpoint**: `POST /api/orders/{orderId}/redo`

**Authentication**: Required (Cookie, owner)

**Path Parameters**:

| Parameter | Type   | Required | Description      |
| --------- | ------ | -------- | ---------------- |
| `orderId` | string | Yes      | Order ID to redo |

**Response** `200 OK` - [Order](#order) (new order)

---

### Add Item to Order

Adds an item to a pending order.

**Endpoint**: `POST /api/orders/{orderId}/items`

**Authentication**: Required (Cookie, owner)

**Path Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `orderId` | string | Yes      | Order ID    |

**Request Body** - [OrderItem](#orderitem):

```json
{
  "productId": "507f1f77bcf86cd799439012",
  "quantity": 2
}
```

**Response** `200 OK` - [Order](#order)

---

### Update Order Item

Updates an item's quantity in a pending order.

**Endpoint**: `PUT /api/orders/{orderId}/items/{productId}`

**Authentication**: Required (Cookie, owner)

**Path Parameters**:

| Parameter   | Type   | Required | Description |
| ----------- | ------ | -------- | ----------- |
| `orderId`   | string | Yes      | Order ID    |
| `productId` | string | Yes      | Product ID  |

**Request Body** - [OrderItem](#orderitem):

```json
{
  "productId": "507f1f77bcf86cd799439012",
  "quantity": 3
}
```

**Response** `200 OK` - [Order](#order)

---

### Remove Item from Order

Removes an item from a pending order.

**Endpoint**: `DELETE /api/orders/{orderId}/items/{productId}`

**Authentication**: Required (Cookie, owner)

**Path Parameters**:

| Parameter   | Type   | Required | Description          |
| ----------- | ------ | -------- | -------------------- |
| `orderId`   | string | Yes      | Order ID             |
| `productId` | string | Yes      | Product ID to remove |

**Response** `200 OK` - [Order](#order)

---

### Clear Cart Items

Removes all items from a pending order.

**Endpoint**: `DELETE /api/orders/{orderId}/items`

**Authentication**: Required (Cookie, owner)

**Path Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `orderId` | string | Yes      | Order ID    |

**Response** `200 OK` - [Order](#order)

---

### Get User Stats

Returns purchase statistics for a user.

**Endpoint**: `GET /api/orders/user/{userId}/stats`

**Authentication**: Required (Cookie)

**Path Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `userId`  | string | Yes      | User ID     |

**Response** `200 OK`:

```json
{
  "totalOrders": 25,
  "totalSpent": 3450.75,
  "lastOrderDate": "2025-02-05T14:22:00Z",
  "mostPurchasedProductId": "507f1f77bcf86cd799439012",
  "mostPurchasedProductName": "Wireless Headphones",
  "mostPurchasedProductCount": 5,
  "totalQuantityBought": 42
}
```

---

### Get Seller Stats

Returns sales statistics for a seller.

**Endpoint**: `GET /api/orders/seller/{sellerId}/stats`

**Authentication**: Required (Cookie)

**Path Parameters**:

| Parameter  | Type   | Required | Description |
| ---------- | ------ | -------- | ----------- |
| `sellerId` | string | Yes      | Seller ID   |

**Response** `200 OK`:

```json
{
  "totalRevenue": 15000.5,
  "totalSales": 150,
  "totalOrders": 120,
  "totalCustomers": 85,
  "lastSaleDate": "2025-02-05T14:22:00Z"
}
```

---

## Media

### Upload File

Uploads a file for a product.

**Endpoint**: `POST /api/media/upload`

**Content-Type**: `multipart/form-data`

**Request Body**:

| Part        | Type   | Required | Description           |
| ----------- | ------ | -------- | --------------------- |
| `file`      | File   | Yes      | File to upload        |
| `productId` | string | Yes      | Associated product ID |

**Response** `200 OK` - [MediaUploadResponseDTO](#mediauploadresponsedto):

```json
{
  "fileId": "507f1f77bcf86cd799439015",
  "fileUrl": "/api/media/files/abc123.jpg"
}
```

---

### Upload Avatar

Uploads an avatar image.

**Endpoint**: `POST /api/media/upload/avatar`

**Content-Type**: `multipart/form-data`

**Request Body**:

| Parameter | Type | Required | Description       |
| --------- | ---- | -------- | ----------------- |
| `file`    | File | Yes      | Avatar image file |

**Response** `200 OK`:

```
/api/media/files/avatar123.jpg
```

---

### Get File

Retrieves a file by filename.

**Endpoint**: `GET /api/media/files/{filename}`

**Path Parameters**:

| Parameter  | Type   | Required | Description |
| ---------- | ------ | -------- | ----------- |
| `filename` | string | Yes      | Filename    |

**Response** `200 OK`: Binary file content with appropriate Content-Type header

---

### Get Product Image URLs

Returns limited image URLs for a product.

**Endpoint**: `GET /api/media/product/{productId}/urls`

**Path Parameters**:

| Parameter   | Type   | Required | Description |
| ----------- | ------ | -------- | ----------- |
| `productId` | string | Yes      | Product ID  |

**Query Parameters**:

| Parameter | Type | Default | Description            |
| --------- | ---- | ------- | ---------------------- |
| `limit`   | int  | 3       | Maximum URLs to return |

**Response** `200 OK`:

```json
[
  "/api/media/files/img1.jpg",
  "/api/media/files/img2.jpg",
  "/api/media/files/img3.jpg"
]
```

---

### Get Media Batch

Returns media for a product.

**Endpoint**: `GET /api/media/batch`

**Query Parameters**:

| Parameter   | Type   | Required | Description |
| ----------- | ------ | -------- | ----------- |
| `productID` | string | Yes      | Product ID  |

**Response** `200 OK` - Array of [MediaUploadResponseDTO](#mediauploadresponsedto):

```json
[
  {
    "fileId": "507f1f77bcf86cd799439015",
    "fileUrl": "/api/media/files/img1.jpg"
  },
  {
    "fileId": "507f1f77bcf86cd799439016",
    "fileUrl": "/api/media/files/img2.jpg"
  }
]
```

---

### Delete Media

Deletes a media file by ID.

**Endpoint**: `DELETE /api/media/{id}`

**Path Parameters**:

| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `id`      | string | Yes      | Media ID    |

**Response** `200 OK`:

```
Delete media successfully
```

---

## Data Models

### Enums

#### Role

```
CLIENT | SELLER | ADMIN
```

#### OrderStatus

```
PENDING | PROCESSING | SHIPPING | SHIPPED | DELIVERED | CANCELLED
```

#### PaymentMethod

```
CARD | PAY_ON_DELIVERY
```

---

### DTOs

#### InfoUserDTO

| Field       | Type   | Description      |
| ----------- | ------ | ---------------- |
| `id`        | string | User ID          |
| `firstName` | string | First name       |
| `lastName`  | string | Last name        |
| `email`     | string | Email address    |
| `avatarUrl` | string | Avatar image URL |
| `role`      | Role   | User role        |

---

#### UpdateUserDTO

| Field             | Type    | Description                                     |
| ----------------- | ------- | ----------------------------------------------- |
| `email`           | string? | New email (optional)                            |
| `firstName`       | string? | New first name (optional)                       |
| `lastName`        | string? | New last name (optional)                        |
| `currentPassword` | string? | Current password (required for password change) |
| `newPassword`     | string? | New password (optional, min 5 chars)            |

---

#### SellerProfileDTO

| Field                     | Type     | Description                  |
| ------------------------- | -------- | ---------------------------- |
| `sellerId`                | string   | Seller ID                    |
| `sellerName`              | string   | Shop name                    |
| `shopLogoUrl`             | string?  | Shop logo URL                |
| `shopDescription`         | string?  | Shop description             |
| `totalRevenue`            | decimal  | Total revenue                |
| `totalSales`              | int      | Total items sold             |
| `totalOrders`             | int      | Total orders received        |
| `totalCustomers`          | int      | Unique customers             |
| `bestSellingProductId`    | string?  | Best selling product ID      |
| `bestSellingProductName`  | string?  | Best selling product name    |
| `bestSellingProductCount` | int      | Best seller sales count      |
| `averageRating`           | double   | Average rating               |
| `totalReviews`            | int      | Total reviews                |
| `totalFiveStarReviews`    | int      | 5-star review count          |
| `isVerified`              | boolean  | Verification status          |
| `isActive`                | boolean  | Active status                |
| `deliveryRating`          | double   | Delivery rating              |
| `communicationRating`     | double   | Communication rating         |
| `returnRate`              | int      | Return rate percentage       |
| `cancellationRate`        | int      | Cancellation rate percentage |
| `joinDate`                | instant  | Join date                    |
| `lastSaleDate`            | instant? | Last sale date               |
| `categories`              | string[] | Product categories           |
| `followerCount`           | int      | Follower count               |

---

#### ProductCardDTO

| Field         | Type     | Description             |
| ------------- | -------- | ----------------------- |
| `id`          | string   | Product ID              |
| `name`        | string   | Product name            |
| `description` | string   | Product description     |
| `price`       | double   | Price                   |
| `quantity`    | int      | Available quantity      |
| `createdByMe` | boolean  | Created by current user |
| `imageUrls`   | string[] | Image URLs              |

---

#### ProductDTO

| Field             | Type                     | Description             |
| ----------------- | ------------------------ | ----------------------- |
| `productId`       | string                   | Product ID              |
| `name`            | string                   | Product name            |
| `description`     | string                   | Product description     |
| `price`           | double                   | Price                   |
| `quantity`        | int                      | Available quantity      |
| `sellerFirstName` | string                   | Seller first name       |
| `sellerLastName`  | string                   | Seller last name        |
| `sellerEmail`     | string                   | Seller email            |
| `createdByMe`     | boolean                  | Created by current user |
| `media`           | MediaUploadResponseDTO[] | Product media           |

---

#### CreateProductDTO

| Field         | Type   | Description                 |
| ------------- | ------ | --------------------------- |
| `name`        | string | Product name (3-100 chars)  |
| `description` | string | Description (min 5 chars)   |
| `price`       | double | Price (positive)            |
| `quantity`    | int    | Quantity (zero or positive) |

---

#### UpdateProductDTO

| Field         | Type    | Description                |
| ------------- | ------- | -------------------------- |
| `name`        | string? | Product name (3-100 chars) |
| `description` | string? | Description (min 5 chars)  |
| `price`       | double? | Price (zero or positive)   |
| `quantity`    | int?    | Quantity (non-negative)    |

---

#### ProductSimpleDTO

| Field         | Type   | Description         |
| ------------- | ------ | ------------------- |
| `productId`   | string | Product ID          |
| `name`        | string | Product name        |
| `description` | string | Product description |
| `price`       | double | Price               |
| `quantity`    | int    | Available quantity  |
| `sellerID`    | string | Seller ID           |

---

#### MediaUploadResponseDTO

| Field     | Type   | Description |
| --------- | ------ | ----------- |
| `fileId`  | string | Media ID    |
| `fileUrl` | string | Media URL   |

---

#### Order

| Field             | Type          | Description           |
| ----------------- | ------------- | --------------------- |
| `id`              | string        | Order ID              |
| `userId`          | string        | User ID               |
| `shippingAddress` | string        | Shipping address      |
| `status`          | OrderStatus   | Order status          |
| `items`           | OrderItem[]   | Order items           |
| `paymentMethod`   | PaymentMethod | Payment method        |
| `orderDate`       | instant?      | Order completion date |
| `createdAt`       | instant       | Creation timestamp    |
| `updatedAt`       | instant       | Last update timestamp |

---

#### OrderItem

| Field         | Type     | Description            |
| ------------- | -------- | ---------------------- |
| `productId`   | string   | Product ID             |
| `quantity`    | int      | Quantity (min 1)       |
| `price`       | decimal? | Price at purchase time |
| `sellerId`    | string?  | Seller ID              |
| `productName` | string?  | Product name           |

---

#### CreateOrderRequest

| Field             | Type          | Description      |
| ----------------- | ------------- | ---------------- |
| `userId`          | string        | User ID          |
| `shippingAddress` | string        | Shipping address |
| `items`           | OrderItem[]   | Order items      |
| `paymentMethod`   | PaymentMethod | Payment method   |

---

#### CheckoutRequest

| Field             | Type          | Description      |
| ----------------- | ------------- | ---------------- |
| `shippingAddress` | string        | Shipping address |
| `paymentMethod`   | PaymentMethod | Payment method   |

---

#### UpdateOrderStatusRequest

| Field    | Type        | Description |
| -------- | ----------- | ----------- |
| `status` | OrderStatus | New status  |

---

#### Page\<T\>

| Field           | Type | Description                     |
| --------------- | ---- | ------------------------------- |
| `content`       | T[]  | Page content                    |
| `totalElements` | long | Total items across all pages    |
| `totalPages`    | int  | Total number of pages           |
| `number`        | int  | Current page number (0-indexed) |

---

#### Product

| Field         | Type    | Description           |
| ------------- | ------- | --------------------- |
| `id`          | string  | Product ID            |
| `name`        | string  | Product name          |
| `description` | string  | Product description   |
| `price`       | double  | Price                 |
| `quantity`    | int     | Available quantity    |
| `sellerID`    | string  | Seller user ID        |
| `createdAt`   | instant | Creation timestamp    |
| `updatedAt`   | instant | Last update timestamp |

---

## HTTP Status Codes

| Code  | Description                                    |
| ----- | ---------------------------------------------- |
| `200` | OK - Request succeeded                         |
| `201` | Created - Resource created                     |
| `204` | No Content - Request succeeded, no body        |
| `400` | Bad Request - Invalid input                    |
| `401` | Unauthorized - Authentication required         |
| `403` | Forbidden - Insufficient permissions           |
| `404` | Not Found - Resource not found                 |
| `409` | Conflict - Resource conflict (e.g., duplicate) |
| `500` | Internal Server Error                          |

---

**Last Updated**: February 6, 2026  
**API Version**: 1.0.0
