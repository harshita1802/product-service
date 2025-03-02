package dev.harshita.EcomProductService.EcomProductService.service;

import dev.harshita.EcomProductService.EcomProductService.dto.requestDto.ProductRequestDto;
import dev.harshita.EcomProductService.EcomProductService.dto.responseDto.CustomProductResponseDto;
import dev.harshita.EcomProductService.EcomProductService.dto.responseDto.ProductResponseDto;
import dev.harshita.EcomProductService.EcomProductService.model.Category;
import dev.harshita.EcomProductService.EcomProductService.model.Product;
import dev.harshita.EcomProductService.EcomProductService.exception.NoProductsFoundException;
import dev.harshita.EcomProductService.EcomProductService.mapper.DtoToEntityMapper;
import dev.harshita.EcomProductService.EcomProductService.mapper.EntityToDtoMapper;
import dev.harshita.EcomProductService.EcomProductService.repository.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryServiceImpl categoryService;
    @Mock
    private EntityToDtoMapper entityToDtoMapper;
    @Mock
    private DtoToEntityMapper dtoToEntityMapper;
    @Mock
    private RedisTemplate<Object,Object> redisTemplate;
    @Mock
    private ValueOperations<Object, Object> valueOperationsMock;

    @InjectMocks
    private CustomProductServiceImpl productService;

    @BeforeEach
    public void initialise(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAddProductSuccessfully(){
        //arrange
        ProductRequestDto productRequestDto = new ProductRequestDto();
        productRequestDto.setCategoryName("Mobile and Accessories");

        Category category = new Category();
        category.setName("Mobile and Accessories");

        Product product = new Product();

        Product savedProduct = new Product();

        CustomProductResponseDto productResponseDto = new CustomProductResponseDto();

        when(categoryService.getByName(productRequestDto.getCategoryName())).thenReturn(category);
        when(dtoToEntityMapper.convertProductRequestDtoToEntity(productRequestDto, category)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(savedProduct);
        when(entityToDtoMapper.convertProductToResponseDto(savedProduct)).thenReturn(productResponseDto);

        //act
        ProductResponseDto result = productService.addProduct(productRequestDto);

        //assert
        verify(categoryService).getByName(productRequestDto.getCategoryName());
        verify(productRepository).save(product);
        verify(dtoToEntityMapper).convertProductRequestDtoToEntity(productRequestDto,category);
        verify(entityToDtoMapper).convertProductToResponseDto(savedProduct);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(productResponseDto,result);
    }

    @Test
    public void testUpdateProductSuccessfully(){

        //arrange
        UUID randomId = UUID.randomUUID();

        ProductRequestDto productRequestDto = new ProductRequestDto();
        productRequestDto.setPrice(100000.00);
        productRequestDto.setDescription("Iphone model");

        Product product = new Product();
        product.setId(randomId);
        product.setName("IPhone 15");
        product.setPrice(150000.00);
        product.setDescription("New Iphone model");

        Product savedProduct = new Product();
        savedProduct.setPrice(productRequestDto.getPrice());
        savedProduct.setDescription(productRequestDto.getDescription());

        CustomProductResponseDto productResponseDto = new CustomProductResponseDto();
        productResponseDto.setPrice(savedProduct.getPrice());
        productResponseDto.setDescription(savedProduct.getDescription());

        when(productRepository.getReferenceById(randomId)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(savedProduct);
        when(entityToDtoMapper.convertProductToResponseDto(savedProduct)).thenReturn(productResponseDto);

        //act
        ProductResponseDto result = productService.updateProduct(randomId,productRequestDto);

        //assert
        verify(productRepository).getReferenceById(randomId);
        verify(productRepository).save(product);
        verify(entityToDtoMapper).convertProductToResponseDto(savedProduct);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(productRequestDto.getPrice(),result.getPrice());
        Assertions.assertEquals(productRequestDto.getDescription(),result.getDescription());
        Assertions.assertEquals(productResponseDto,result);
    }

    @Test
    public void testDeleteProductSuccessfully(){

        //arrange
        UUID randomId = UUID.randomUUID();

        //act
        boolean result = productService.deleteProduct(randomId);

        //assert
        Assertions.assertEquals(true,result);
    }

    @Test
    public void testGetAllProductsSuccessfully(){
        //arrange
        List<Product> products = new ArrayList<>();
        products.add(new Product());

        CustomProductResponseDto productResponseDto = new CustomProductResponseDto();

        List<ProductResponseDto> productResponseDtoList = new ArrayList<>();
        productResponseDtoList.add(productResponseDto);

        when(productRepository.findAll()).thenReturn(products);
        when(entityToDtoMapper.convertProductToResponseDto(products.get(0))).thenReturn(productResponseDto);

        //act
        List<CustomProductResponseDto> result = productService.getAllProducts();

        //assert
        verify(productRepository).findAll();
        verify(entityToDtoMapper).convertProductToResponseDto(products.get(0));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(productResponseDtoList, result);
    }

    @Test
    public void testGetAllProductsWhenFindAllReturnsNull(){
        //arrange
        when(productRepository.findAll()).thenReturn(null);

        //act and assert
        Assertions.assertThrows(NoProductsFoundException.class,() -> productService.getAllProducts());
    }

    @Test
    public void testGetAllProductsWhenNoProductFound(){
        //arrange
        when(productRepository.findAll()).thenReturn(new ArrayList<>());

        //act and assert
        Assertions.assertThrows(NoProductsFoundException.class,() -> productService.getAllProducts());
    }

    @Test
    public void testGetProductByIdSuccessfully(){
        //arrange
        UUID prodId = UUID.randomUUID();

        Product product = new Product();
        product.setId(prodId);

        CustomProductResponseDto productResponseDto = new CustomProductResponseDto();

        when(redisTemplate.hasKey(prodId)).thenReturn(false);
        when(productRepository.getReferenceById(prodId)).thenReturn(product);
        when(entityToDtoMapper.convertProductToResponseDto(product)).thenReturn(productResponseDto);
        when(redisTemplate.opsForValue()).thenReturn(valueOperationsMock);

        //act
        ProductResponseDto result = productService.getById(prodId);

        //assert
        verify(redisTemplate).hasKey(prodId);
        verify(productRepository).getReferenceById(prodId);
        verify(entityToDtoMapper).convertProductToResponseDto(product);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(productResponseDto,result);
    }

    @Test
    public void testGetProductByIdCachedByRedis(){
        //arrange
        UUID prodId = UUID.randomUUID();

        CustomProductResponseDto productResponseDto = new CustomProductResponseDto();

        when(redisTemplate.hasKey(prodId)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperationsMock);
        when(valueOperationsMock.get(prodId)).thenReturn(productResponseDto);

        //act
        ProductResponseDto result = productService.getById(prodId);

        //assert
        verify(redisTemplate).hasKey(prodId);
        verify(redisTemplate).opsForValue();
        verify(valueOperationsMock).get(prodId);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(productResponseDto,result);
    }
}
