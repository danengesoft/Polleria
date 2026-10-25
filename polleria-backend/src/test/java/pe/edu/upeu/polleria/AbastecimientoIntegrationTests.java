package pe.edu.upeu.polleria;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.edu.upeu.polleria.abastecimiento.insumo.dto.*;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.UnidadMedida;
import pe.edu.upeu.polleria.abastecimiento.insumo.service.InsumoService;
import pe.edu.upeu.polleria.abastecimiento.proveedor.dto.*;
import pe.edu.upeu.polleria.abastecimiento.proveedor.service.ProveedorService;
import pe.edu.upeu.polleria.abastecimiento.compra.dto.*;
import pe.edu.upeu.polleria.abastecimiento.compra.service.CompraService;
import pe.edu.upeu.polleria.abastecimiento.inventario.dto.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.entity.TipoMovimiento;
import pe.edu.upeu.polleria.abastecimiento.inventario.service.InventarioService;
import pe.edu.upeu.polleria.exception.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

// Fixtures are committed so rollback/concurrency is checked across real Oracle transactions.
// Cleanup targets ONLY rows belonging to the exact IDs created in this test.
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
class AbastecimientoIntegrationTests {
 @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;
 @Autowired InsumoService insumos;@Autowired ProveedorService proveedores;@Autowired CompraService compras;@Autowired InventarioService inventario;
 Long a,b,proveedor;List<Long> proveedoresCreados;List<Long> insumosCreados;
 String base="/api/v1/abastecimiento";
 BigDecimal n(String v){return new BigDecimal(v);}
 RequestPostProcessor rol(String r){return jwt().jwt(j->j.subject("f14-test")).authorities(new SimpleGrantedAuthority("ROLE_"+r));}
 InsumoRequest insumo(String nombre,UnidadMedida unidad,boolean activo){return new InsumoRequest(nombre,"Descripción",unidad,activo);}
 ProveedorRequest prov(String ruc,boolean activo){return new ProveedorRequest("Proveedor F14",ruc,null,null,null,activo);}
 Long nuevoProveedor(String ruc){Long id=proveedores.crear(prov(ruc,true)).id();proveedoresCreados.add(id);return id;}
 Long nuevoInsumo(String nombre,UnidadMedida unidad){Long id=insumos.crear(insumo(nombre,unidad,true)).id();insumosCreados.add(id);return id;}
 @BeforeEach void preparar(){proveedoresCreados=new ArrayList<>();insumosCreados=new ArrayList<>();a=nuevoInsumo("Aceite",UnidadMedida.L);b=nuevoInsumo("Papas",UnidadMedida.KG);proveedor=nuevoProveedor(null);}
 @AfterEach void limpiar(){
  for(Long id:insumosCreados)jdbc.update("DELETE FROM POLLERIA.MOVIMIENTOS_INVENTARIO WHERE INSUMO_ID=?",id);
  for(Long id:proveedoresCreados){jdbc.update("DELETE FROM POLLERIA.DETALLE_COMPRAS WHERE COMPRA_ID IN(SELECT ID FROM POLLERIA.COMPRAS WHERE PROVEEDOR_ID=?)",id);jdbc.update("DELETE FROM POLLERIA.COMPRAS WHERE PROVEEDOR_ID=?",id);jdbc.update("DELETE FROM POLLERIA.PROVEEDORES WHERE ID=?",id);}
  for(Long id:insumosCreados)jdbc.update("DELETE FROM POLLERIA.INSUMOS WHERE ID=?",id);
 }
 DetalleCompraRequest linea(Long id,String cantidad,String costo){return new DetalleCompraRequest(id,n(cantidad),n(costo));}
 CompraResponse comprar(DetalleCompraRequest... lineas){return compras.crear(new CompraRequest(proveedor,List.of(lineas)));}
 void stock(Long id,String esperado){assertThat(insumos.obtener(id).stockActual()).isEqualByComparingTo(esperado);}
 long movimientos(Long id){return jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.MOVIMIENTOS_INVENTARIO WHERE INSUMO_ID=?",Long.class,id);}
 String jsonCompra(String cantidad,String costo){return "{\"proveedorId\":"+proveedor+",\"detalles\":[{\"insumoId\":"+a+",\"cantidad\":"+cantidad+",\"costoUnitario\":"+costo+"}]}";}
 @Test void insumoIniciaEnCeroYRequestNoPuedeElegirStock() throws Exception {
  String unique="Stock-"+UUID.randomUUID();
  try{mvc.perform(post(base+"/insumos").with(rol("ABASTECIMIENTO")).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\""+unique+"\",\"unidadMedida\":\"L\",\"activo\":true,\"stockActual\":500}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.stockActual").value(0));
  }finally{jdbc.update("DELETE FROM POLLERIA.INSUMOS WHERE NOMBRE=?",unique);}
 }
 @Test void edicionNoCambiaStockYNormalizaNombre() throws Exception {
  inventario.ajuste(new AjusteRequest(a,n("5.123"),"Conteo"));
  mvc.perform(put(base+"/insumos/{id}",a).with(rol("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\" Aceite   vegetal \",\"descripcion\":\"Nueva\",\"unidadMedida\":\"L\",\"activo\":false,\"stockActual\":999}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.stockActual").value(5.123)).andExpect(jsonPath("$.nombre").value("Aceite vegetal")).andExpect(jsonPath("$.activo").value(false));
 }
 @Test void unidadSoloCambiaSinHistorialYConStockCero(){assertThat(insumos.actualizar(a,insumo("Aceite",UnidadMedida.ML,true)).unidadMedida()).isEqualTo(UnidadMedida.ML);inventario.ajuste(new AjusteRequest(a,n("1"),"Conteo"));inventario.ajuste(new AjusteRequest(a,n("0"),"Sin saldo"));assertThatThrownBy(()->insumos.actualizar(a,insumo("Aceite",UnidadMedida.L,true))).isInstanceOf(BusinessConflictException.class);}
 @Test void unidadInvalidaRechazadaYOracleImpideStockNegativo() throws Exception {mvc.perform(put(base+"/insumos/{id}",a).with(rol("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Aceite\",\"unidadMedida\":\"CAJA\",\"activo\":true}")) .andExpect(status().isBadRequest());assertThatThrownBy(()->jdbc.update("UPDATE POLLERIA.INSUMOS SET STOCK_ACTUAL=-1 WHERE ID=?",a)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);stock(a,"0");}
 @Test void proveedorRucOpcionalActualizaNormalizaYDesactiva(){var r=proveedores.actualizar(proveedor,new ProveedorRequest("  Proveedor   actualizado  ",null," 999000014 "," TEST@EXAMPLE.COM "," Dirección ",false));assertThat(r.razonSocial()).isEqualTo("Proveedor actualizado");assertThat(r.correo()).isEqualTo("test@example.com");assertThat(r.ruc()).isNull();assertThat(r.activo()).isFalse();}
 @Test void rucDuplicado409YUniqueReal() throws Exception {
  String ruc="20"+String.format("%09d",Math.abs(UUID.randomUUID().getLeastSignificantBits()%1000000000));Long id=nuevoProveedor(ruc);
  mvc.perform(post(base+"/proveedores").with(rol("ABASTECIMIENTO")).contentType(MediaType.APPLICATION_JSON).content("{\"razonSocial\":\"Otro\",\"ruc\":\" "+ruc+" \",\"activo\":true}")) .andExpect(status().isConflict());
  assertThatThrownBy(()->jdbc.update("UPDATE POLLERIA.PROVEEDORES SET RUC=? WHERE ID=?",ruc,proveedor)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
 }
 @Test void rucInvalido400() throws Exception {mvc.perform(put(base+"/proveedores/{id}",proveedor).with(rol("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content("{\"razonSocial\":\"Proveedor\",\"ruc\":\"1234\",\"activo\":true}")) .andExpect(status().isBadRequest());}
 @Test void mismoTelefonoNoBloqueaProveedores(){var r=new ProveedorRequest("Otro",null,"999000014",null,null,true);proveedores.actualizar(proveedor,r);Long id=proveedores.crear(r).id();proveedoresCreados.add(id);assertThat(id).isNotEqualTo(proveedor);}
 @Test void compraConDosDetallesCalculaMonedaGeneraEntradasYStock(){var c=comprar(linea(a,"2.500","12.3456"),linea(b,"3.125","4.2000"));assertThat(c.detalles()).hasSize(2);assertThat(c.detalles().get(0).subtotal()).isEqualByComparingTo("30.86");assertThat(c.detalles().get(1).subtotal()).isEqualByComparingTo("13.13");assertThat(c.total()).isEqualByComparingTo("43.99");stock(a,"2.500");stock(b,"3.125");var f=new MovimientoFiltro();f.setInsumoId(a);var m=inventario.buscar(f).getFirst();assertThat(m.tipo()).isEqualTo(TipoMovimiento.ENTRADA);assertThat(m.compraId()).isEqualTo(c.id());assertThat(m.stockAnterior()).isEqualByComparingTo("0");assertThat(m.stockPosterior()).isEqualByComparingTo("2.5");}
 @Test void totalNombreUnidadYFechaDelRequestNoTienenAutoridad() throws Exception {mvc.perform(post(base+"/compras").with(rol("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(jsonCompra("2","3.25").replace("{\"proveedorId\"","{\"total\":0.01,\"fecha\":\"2000-01-01T00:00:00\",\"proveedorId\"").replace("\"cantidad\":2","\"nombreInsumo\":\"Falso\",\"unidadMedida\":\"KG\",\"subtotal\":0.01,\"cantidad\":2")))
  .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(6.5)).andExpect(jsonPath("$.detalles[0].nombreInsumo").value("Aceite")).andExpect(jsonPath("$.detalles[0].unidadMedida").value("L"));}
 @Test void proveedorInexistenteOInactivoNoRegistraCompra(){assertThatThrownBy(()->compras.crear(new CompraRequest(Long.MAX_VALUE,List.of(linea(a,"1","1"))))).isInstanceOf(ResourceNotFoundException.class);proveedores.actualizar(proveedor,prov(null,false));assertThatThrownBy(()->comprar(linea(a,"1","1"))).isInstanceOf(BusinessConflictException.class);stock(a,"0");}
 @Test void insumoInactivoNoRegistraCompra(){insumos.actualizar(a,insumo("Aceite",UnidadMedida.L,false));assertThatThrownBy(()->comprar(linea(a,"1","1"))).isInstanceOf(BusinessConflictException.class);}
 @Test void rollbackCompraConSegundaLineaInexistenteNoDejaFilasNiStock(){assertThatThrownBy(()->comprar(linea(a,"5","2"),linea(Long.MAX_VALUE,"1","1"))).isInstanceOf(ResourceNotFoundException.class);stock(a,"0");assertSinCompra();assertThat(movimientos(a)).isZero();}
 void assertSinCompra(){assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.COMPRAS WHERE PROVEEDOR_ID=?",Long.class,proveedor)).isZero();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.DETALLE_COMPRAS WHERE INSUMO_ID IN(?,?)",Long.class,a,b)).isZero();}
 @Test void rollbackTardioDespuesDeFlushRevierteCompraDetallesEntradaYStock(){inventario.ajuste(new AjusteRequest(b,n("9999999999999999.999"),"Conteo límite de prueba"));assertThatThrownBy(()->comprar(linea(a,"5","2"),linea(b,"1","1"))).isInstanceOf(InvalidRequestException.class);stock(a,"0");stock(b,"9999999999999999.999");assertSinCompra();assertThat(movimientos(a)).isZero();assertThat(movimientos(b)).isEqualTo(1);}
 @ParameterizedTest @ValueSource(strings={"0","-1","0.0001"}) void cantidadCompraInvalida400(String cantidad) throws Exception {mvc.perform(post(base+"/compras").with(rol("ABASTECIMIENTO")).contentType(MediaType.APPLICATION_JSON).content(jsonCompra(cantidad,"1"))).andExpect(status().isBadRequest());stock(a,"0");}
 @ParameterizedTest @ValueSource(strings={"0","-2","0.00001"}) void costoCompraInvalido400(String costo) throws Exception {mvc.perform(post(base+"/compras").with(rol("ABASTECIMIENTO")).contentType(MediaType.APPLICATION_JSON).content(jsonCompra("1",costo))).andExpect(status().isBadRequest());}
 @Test void sinDetallesYDuplicadosNoRegistranCompra() throws Exception {mvc.perform(post(base+"/compras").with(rol("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content("{\"proveedorId\":"+proveedor+",\"detalles\":[]}")) .andExpect(status().isBadRequest());assertThatThrownBy(()->comprar(linea(a,"1","1"),linea(a,"1","2"))).isInstanceOf(InvalidRequestException.class);assertSinCompra();}
 @Test void fotografiasCompraPermanecenTrasRenombrarInsumoYDesactivarProveedor(){var c=comprar(linea(a,"1","2"));insumos.actualizar(a,insumo("Aceite vegetal",UnidadMedida.L,true));proveedores.actualizar(proveedor,prov(null,false));var r=compras.obtener(c.id());assertThat(r.detalles().getFirst().nombreInsumo()).isEqualTo("Aceite");assertThat(r.detalles().getFirst().unidadMedida()).isEqualTo(UnidadMedida.L);assertThat(r.proveedor().id()).isEqualTo(proveedor);assertThatThrownBy(()->jdbc.update("DELETE FROM POLLERIA.PROVEEDORES WHERE ID=?",proveedor)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);assertThatThrownBy(()->jdbc.update("DELETE FROM POLLERIA.INSUMOS WHERE ID=?",a)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);}
 @Test void salidaValidaConservaStockAnteriorPosterior(){inventario.ajuste(new AjusteRequest(a,n("10.250"),"Conteo"));var m=inventario.salida(new SalidaRequest(a,n("2.125"),"Uso interno"));assertThat(m.tipo()).isEqualTo(TipoMovimiento.SALIDA);assertThat(m.stockAnterior()).isEqualByComparingTo("10.250");assertThat(m.stockPosterior()).isEqualByComparingTo("8.125");assertThat(m.compraId()).isNull();stock(a,"8.125");}
 @Test void salidaInsuficienteRevierteSinMovimiento(){inventario.ajuste(new AjusteRequest(a,n("1"),"Conteo"));long antes=movimientos(a);assertThatThrownBy(()->inventario.salida(new SalidaRequest(a,n("2"),"Merma"))).isInstanceOf(BusinessConflictException.class);stock(a,"1");assertThat(movimientos(a)).isEqualTo(antes);}
 @ParameterizedTest @ValueSource(strings={"0","-1","0.0001"}) void salidaInvalida400(String cantidad) throws Exception {mvc.perform(post(base+"/movimientos/salidas").with(rol("ABASTECIMIENTO")).contentType(MediaType.APPLICATION_JSON).content("{\"insumoId\":"+a+",\"cantidad\":"+cantidad+",\"motivo\":\"Uso\"}")) .andExpect(status().isBadRequest());stock(a,"0");}
 @Test void salidaYAjusteExigenMotivo() throws Exception {for(String op:List.of("salidas","ajustes"))mvc.perform(post(base+"/movimientos/"+op).with(rol("ABASTECIMIENTO")).contentType(MediaType.APPLICATION_JSON).content("{\"insumoId\":"+a+",\"cantidad\":1,\"nuevoStock\":1,\"motivo\":\"  \"}")) .andExpect(status().isBadRequest());}
 @Test void ajusteSubeBajaYPermiteCeroConDiferenciaFirmada(){var arriba=inventario.ajuste(new AjusteRequest(a,n("8.500"),"Conteo"));assertThat(arriba.cantidad()).isEqualByComparingTo("8.5");var abajo=inventario.ajuste(new AjusteRequest(a,n("2.125"),"Nuevo conteo"));assertThat(abajo.cantidad()).isEqualByComparingTo("-6.375");assertThat(abajo.stockAnterior()).isEqualByComparingTo("8.5");inventario.ajuste(new AjusteRequest(a,n("0"),"Sin saldo"));stock(a,"0");}
 @Test void ajusteNegativo400YCantidadNoElegidaPorAngular() throws Exception {mvc.perform(post(base+"/movimientos/ajustes").with(rol("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content("{\"insumoId\":"+a+",\"nuevoStock\":-1,\"motivo\":\"Conteo\"}")) .andExpect(status().isBadRequest());mvc.perform(post(base+"/movimientos/ajustes").with(rol("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content("{\"insumoId\":"+a+",\"nuevoStock\":2.5,\"cantidad\":999,\"stockAnterior\":500,\"stockPosterior\":999,\"motivo\":\"Conteo\"}")) .andExpect(status().isCreated()).andExpect(jsonPath("$.stockAnterior").value(0)).andExpect(jsonPath("$.stockPosterior").value(2.5)).andExpect(jsonPath("$.cantidad").value(2.5));}
 @Test void ajusteSinCambioNoGeneraMovimiento(){assertThatThrownBy(()->inventario.ajuste(new AjusteRequest(a,n("0"),"Conteo"))).isInstanceOf(BusinessConflictException.class);assertThat(movimientos(a)).isZero();}
 @Test void insumoInactivoNoAdmiteMovimientos(){insumos.actualizar(a,insumo("Aceite",UnidadMedida.L,false));assertThatThrownBy(()->inventario.ajuste(new AjusteRequest(a,n("1"),"Conteo"))).isInstanceOf(BusinessConflictException.class);}
 @Test void filtrosDeMovimientosYComprasFuncionanYRechazanRangoInverso() throws Exception {var c=comprar(linea(a,"2","3"));mvc.perform(get(base+"/movimientos").param("insumoId",a.toString()).param("tipo","ENTRADA").with(rol("ABASTECIMIENTO"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].compraId").value(c.id()));mvc.perform(get(base+"/compras").param("proveedorId",proveedor.toString()).with(rol("ABASTECIMIENTO"))).andExpect(jsonPath("$.length()").value(1));mvc.perform(get(base+"/compras").param("fechaDesde","2026-10-25").param("fechaHasta","2026-10-24").with(rol("ABASTECIMIENTO"))).andExpect(status().isBadRequest());}
 @Test void noExistenMutacionesDestructivasDeCompraNiCrudDeStock() throws Exception {var c=comprar(linea(a,"1","1"));for(var request:List.of(put(base+"/compras/{id}",c.id()),delete(base+"/compras/{id}",c.id()),delete(base+"/proveedores/{id}",proveedor),delete(base+"/insumos/{id}",a)))mvc.perform(request.with(rol("ADMINISTRADOR"))).andExpect(status().isMethodNotAllowed());}
 @ParameterizedTest @ValueSource(strings={"ADMINISTRADOR","ABASTECIMIENTO"}) void rolesPermitidos(String r) throws Exception {mvc.perform(get(base+"/insumos").with(rol(r))).andExpect(status().isOk());mvc.perform(get(base+"/compras").with(rol(r))).andExpect(status().isOk());}
 @ParameterizedTest @ValueSource(strings={"CAJERO","REPARTIDOR","CLIENTE"}) void rolesAjenos403(String r) throws Exception {for(String p:List.of("insumos","proveedores","compras","movimientos"))mvc.perform(get(base+"/"+p).with(rol(r))).andExpect(status().isForbidden());}
 @Test void sinToken401() throws Exception {mvc.perform(get(base+"/insumos")).andExpect(status().isUnauthorized());}
 @Test void salidasConcurrentesNoProducenStockNegativoNiMovimientosIncompatibles() throws Exception {
  inventario.ajuste(new AjusteRequest(a,n("10"),"Conteo inicial"));CountDownLatch preparados=new CountDownLatch(2),inicio=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){List<Future<Boolean>> futures=new ArrayList<>();for(int i=0;i<2;i++)futures.add(pool.submit(()->{preparados.countDown();inicio.await();try{inventario.salida(new SalidaRequest(a,n("8"),"Salida concurrente"));return true;}catch(BusinessConflictException ex){return false;}}));assertThat(preparados.await(5,TimeUnit.SECONDS)).isTrue();inicio.countDown();int exitos=0;for(var f:futures)if(f.get(15,TimeUnit.SECONDS))exitos++;assertThat(exitos).isEqualTo(1);}
  stock(a,"2");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.MOVIMIENTOS_INVENTARIO WHERE INSUMO_ID=? AND TIPO='SALIDA'",Long.class,a)).isEqualTo(1);var f=new MovimientoFiltro();f.setInsumoId(a);f.setTipo(TipoMovimiento.SALIDA);var m=inventario.buscar(f).getFirst();assertThat(m.stockAnterior()).isEqualByComparingTo("10");assertThat(m.stockPosterior()).isEqualByComparingTo("2");
 }
}
