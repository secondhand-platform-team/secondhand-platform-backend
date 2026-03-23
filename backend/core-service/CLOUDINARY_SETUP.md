# Image Upload with Cloudinary - Integration Guide

## Setup Complete ✅

The application now supports image uploads to **Cloudinary** cloud storage.

## Components Created

### 1. **CloudinaryService** (`src/main/java/com/secondhand/coreservice/service/CloudinaryService.java`)

- Handles image uploads to Cloudinary
- Supports single and multiple image uploads
- Provides image deletion functionality
- Secure API credential handling

### 2. **ImageUploadController** (`src/main/java/com/secondhand/coreservice/controller/ImageUploadController.java`)

- Exposes REST API endpoints for image uploads
- Provides response DTOs with upload status and image URLs

### 3. **Configuration**

- Added Cloudinary properties to `application.properties`
- Supports environment variables for secure credential management

### 4. **Updated Models**

- **ItemImage**: Added `cloudinaryPublicId` field to track uploaded images for deletion

## API Endpoints

### 1. Upload Single Image

**POST** `/api/images/upload`

**Request:**

```
multipart/form-data
- file: <image file>
```

**Response:**

```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "imageUrl": "https://res.cloudinary.com/dgachkyc7/image/upload/v1234567890/secondhand-items/filename.jpg"
}
```

### 2. Upload Multiple Images

**POST** `/api/images/upload-multiple`

**Request:**

```
multipart/form-data
- files: <image file 1>
- files: <image file 2>
- files: <image file 3>
```

**Response:**

```json
{
  "success": true,
  "message": "Images uploaded successfully",
  "imageUrls": [
    "https://res.cloudinary.com/.../image1.jpg",
    "https://res.cloudinary.com/.../image2.jpg",
    "https://res.cloudinary.com/.../image3.jpg"
  ]
}
```

## Configuration

### Environment Variables (Production)

```properties
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

### application.properties (Development)

```properties
cloudinary.cloud-name=dgachkyc7
cloudinary.api-key=421262947271889
cloudinary.api-secret=2YJG_3cmYpWjGX7BW3q598wWQ3g
cloudinary.folder=secondhand-items
```

## Usage in Frontend

### Example 1: Single Image Upload (JavaScript/Fetch)

```javascript
const formData = new FormData();
formData.append("file", imageFile);

const response = await fetch("/api/images/upload", {
  method: "POST",
  body: formData,
});

const data = await response.json();
if (data.success) {
  console.log("Image URL:", data.imageUrl);
}
```

### Example 2: Multiple Images Upload (JavaScript/Fetch)

```javascript
const formData = new FormData();
imageFiles.forEach((file) => {
  formData.append("files", file);
});

const response = await fetch("/api/images/upload-multiple", {
  method: "POST",
  body: formData,
});

const data = await response.json();
if (data.success) {
  console.log("Image URLs:", data.imageUrls);
}
```

### Example 3: Upload Images with Item Creation

```javascript
// Step 1: Upload images
const imageUrls = [];
for (let file of imageFiles) {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch("/api/images/upload", {
    method: "POST",
    body: formData,
  });
  const data = await res.json();
  imageUrls.push(data.imageUrl);
}

// Step 2: Create item with image URLs
const itemRequest = {
  title: "iPhone 13",
  description: "Like new condition",
  categoryId: "cg-0005",
  price: 500,
  condition: "LIKE_NEW",
  transactionType: "BUY",
  itemImageList: imageUrls.map((url, index) => ({
    imageUrl: url,
    isPrimary: index === 0,
  })),
};

const response = await fetch("/api/items", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(itemRequest),
});
```

## Integration with Item Creation

### Current Flow:

1. Frontend uploads image(s) using `/api/images/upload` or `/api/images/upload-multiple`
2. Get back Cloudinary URLs
3. Include URLs in ItemRequest when creating/updating item
4. ItemImage records store the URL and publication

### Future Enhancements:

- Direct file upload in ItemRequest (multipart support)
- Batch image processing
- Image optimization/resizing via Cloudinary
- Scheduled cleanup of orphaned images

## Error Handling

### Common Errors:

```json
{
  "success": false,
  "message": "File is empty",
  "imageUrl": null
}
```

```json
{
  "success": false,
  "message": "Failed to upload image: Invalid file type",
  "imageUrl": null
}
```

## Security Considerations

1. **Credentials**: Store API credentials in environment variables, never commit to Git
2. **File Validation**: Validate file type and size on both frontend and backend
3. **CORS**: Currently enabled for all origins (`*`), restrict in production
4. **Rate Limiting**: Consider implementing rate limiting for upload endpoints

## Testing with cURL

```bash
# Single image upload
curl -X POST http://localhost:8082/api/images/upload \
  -F "file=@/path/to/image.jpg"

# Multiple images
curl -X POST http://localhost:8082/api/images/upload-multiple \
  -F "files=@/path/to/image1.jpg" \
  -F "files=@/path/to/image2.jpg"
```

## Troubleshooting

1. **"Invalid cloud credentials"**: Check environment variables or application.properties
2. **"File upload failed"**: Check file size, type, and permissions
3. **"Connection timeout"**: Verify internet connection and Cloudinary API availability
4. **Missing images**: Ensure ItemImageList is populated with URLs before creating item

## Dependencies

- `com.cloudinary:cloudinary-core:2.3.2` (already in pom.xml)
- Spring Boot Multipart file handling (built-in)

## Next Steps

1. Update frontend to use new upload endpoints
2. Implement image optimization/cropping
3. Add image deletion when items are deleted
4. Add batch upload progress tracking
5. Implement image gallery/carousel UI
