package guru.springframework.repositories;

import guru.springframework.domain.UnitOfMeasure;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.assertEquals;

import java.util.Optional;

@RunWith(SpringRunner.class)
@DataJpaTest
public class UnitOfMeasureRepositoryIT {
    @Autowired
    UnitOfMeasureRepository unitOfMeasureRepository ;
    @Before
   public void setUp() {
    }

    @Test
   public void findByDescription() throws Exception {
        Optional<UnitOfMeasure> optionalUnitOfMeasure = unitOfMeasureRepository.findByDescription("Teaspoon");
        assertEquals("Teaspoon",optionalUnitOfMeasure.get().getDescription());
    }

//  @DirtiesContext will reload the context again
    @Test
    @DirtiesContext
    public void findByCupDescription() throws Exception {
        Optional<UnitOfMeasure> optionalUnitOfMeasure = unitOfMeasureRepository.findByDescription("Cup");
        assertEquals("Cup",optionalUnitOfMeasure.get().getDescription());
    }

    @Test
    public void findByAnotherCupDescription() throws Exception {
        Optional<UnitOfMeasure> optionalUnitOfMeasure = unitOfMeasureRepository.findByDescription("Cup");
        assertEquals("Cup",optionalUnitOfMeasure.get().getDescription());
    }
}