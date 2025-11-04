package org.somda.sdc.glue.common.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlUtf8Test {

    @Test
    void testValid() {
        {
            // unescaped characters
            var inputData = "abcdefg-._~!$&'()*+,;=:@";
            var encoded = UrlUtf8.encodePChars(inputData);
            var decoded = UrlUtf8.decodePChars(encoded);

            assertEquals(inputData, encoded);
            assertEquals(inputData, decoded);
        }
        {
            // escaped characters
            var inputData = "/////";
            var encoded = UrlUtf8.encodePChars(inputData);
            var decoded = UrlUtf8.decodePChars(encoded);

            assertEquals(inputData, decoded);
        }
        {
            // naughty characters
            var inputData = "Ṱ̺̺̕o͞ ̷i̲̬͇̪͙n̝̗͕v̟̜̘̦͟o̶̙̰̠kè͚̮̺̪̹̱̤ ̖t̝͕̳̣̻̪͞h̼͓̲̦̳̘̲e͇̣̰̦̬͎ ̢̼̻̱̘h͚͎͙̜̣̲ͅi̦̲̣̰̤v̻͍e̺̭̳̪̰-m̢iͅn̖̺̞̲̯̰d̵̼̟͙̩̼̘̳ ̞̥̱̳̭r̛̗̘e͙p͠r̼̞̻̭̗e̺̠̣͟s̘͇̳͍̝͉e͉̥̯̞̲͚̬͜ǹ̬͎͎̟̖͇̤t͍̬̤͓̼̭͘ͅi̪̱n͠g̴͉ ͏͉ͅc̬̟h͡a̫̻̯͘o̫̟̖͍̙̝͉s̗̦̲.̨̹͈̣\n" +
                    "̡͓̞ͅI̗̘̦͝n͇͇͙v̮̫ok̲̫̙͈i̖͙̭̹̠̞n̡̻̮̣̺g̲͈͙̭͙̬͎ ̰t͔̦h̞̲e̢̤ ͍̬̲͖f̴̘͕̣è͖ẹ̥̩l͖͔͚i͓͚̦͠n͖͍̗͓̳̮g͍ ̨o͚̪͡f̘̣̬ ̖̘͖̟͙̮c҉͔̫͖͓͇͖ͅh̵̤̣͚͔á̗̼͕ͅo̼̣̥s̱͈̺̖̦̻͢.̛̖̞̠̫̰\n" +
                    "̗̺͖̹̯͓Ṯ̤͍̥͇͈h̲́e͏͓̼̗̙̼̣͔ ͇̜̱̠͓͍ͅN͕͠e̗̱z̘̝̜̺͙p̤̺̹͍̯͚e̠̻̠͜r̨̤͍̺̖͔̖̖d̠̟̭̬̝͟i̦͖̩͓͔̤a̠̗̬͉̙n͚͜ ̻̞̰͚ͅh̵͉i̳̞v̢͇ḙ͎͟-҉̭̩̼͔m̤̭̫i͕͇̝̦n̗͙ḍ̟ ̯̲͕͞ǫ̟̯̰̲͙̻̝f ̪̰̰̗̖̭̘͘c̦͍̲̞͍̩̙ḥ͚a̮͎̟̙͜ơ̩̹͎s̤.̝̝ ҉Z̡̖̜͖̰̣͉̜a͖̰͙̬͡l̲̫̳͍̩g̡̟̼̱͚̞̬ͅo̗͜.̟\n" +
                    "̦H̬̤̗̤͝e͜ ̜̥̝̻͍̟́w̕h̖̯͓o̝͙̖͎̱̮ ҉̺̙̞̟͈W̷̼̭a̺̪͍į͈͕̭͙̯̜t̶̼̮s̘͙͖̕ ̠̫̠B̻͍͙͉̳ͅe̵h̵̬͇̫͙i̹͓̳̳̮͎̫̕n͟d̴̪̜̖ ̰͉̩͇͙̲͞ͅT͖̼͓̪͢h͏͓̮̻e̬̝̟ͅ ̤̹̝W͙̞̝͔͇͝ͅa͏͓͔̹̼̣l̴͔̰̤̟͔ḽ̫.͕\n" +
                    "Z̮̞̠͙͔ͅḀ̗̞͈̻̗Ḷ͙͎̯̹̞͓G̻O̭̗̮";
            var encoded = UrlUtf8.encodePChars(inputData);
            var decoded = UrlUtf8.decodePChars(encoded);

            assertEquals(inputData, decoded);
        }
        {
            // naughty characters
            var inputData = "ü";
            var encoded = UrlUtf8.encodePChars(inputData);
            var decoded = UrlUtf8.decodePChars(encoded);

            assertEquals(inputData, decoded);
        }
        {
            // don't escape ampersand
            var inputData = "&";
            var encoded = UrlUtf8.encodePChars(inputData);
            var decoded = UrlUtf8.decodePChars(encoded);

            assertEquals(inputData, encoded);
            assertEquals(inputData, decoded);
        }
        {
            // escape ampersand
            var inputData = "abcdefg-._~!$&'()*+,;=:@";
            var encoded = UrlUtf8.encodePChars(inputData, true);
            var decoded = UrlUtf8.decodePChars(encoded);

            assertEquals("abcdefg-._~!$%26'()*+,;=:@", encoded);
            assertEquals(inputData, decoded);
        }
    }

    @Test
    void testUnnecessarilyEncodedCharacters() {
        var encoded = "%41%42%43";
        var decoded = UrlUtf8.decodePChars(encoded);

        assertEquals("ABC", decoded);
    }

}
