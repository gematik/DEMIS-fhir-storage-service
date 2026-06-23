# BundleReadControllerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**findById**](BundleReadControllerApi.md#findById) | **GET** /Bundle/{id} |  |
| [**search**](BundleReadControllerApi.md#search) | **GET** /Bundle |  |


<a name="findById"></a>
# **findById**
> Object findById(headers, id)



### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **headers** | [**HttpHeaders**](../Models/.md)|  | [default to null] |
| **id** | **UUID**|  | [default to null] |

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

<a name="search"></a>
# **search**
> Object search(headers, requestParams)



### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **headers** | [**HttpHeaders**](../Models/.md)|  | [default to null] |
| **requestParams** | [**MultiValueMapStringString**](../Models/List.md)|  | [default to null] |

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

