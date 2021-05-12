using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Threading.Tasks;

namespace OSRSItemScraper
{
    class Program
    {
        // https://oldschool.runescape.wiki/w/Item_spawn
        // https://www.osrsbox.com/osrsbox-db/items-search.json
        static Dictionary<string, string> knownConversion = new Dictionary<string, string>()
        {
            {"Attack potion","Attack potion(1)"},
            {"Blue hat (Legends' Quest)","740"},
            {"Bones (Ape Atoll)","-1"},
            {"Burnt fish (anchovies)","323"},
            {"Burnt fish (herring)","357"},
            {"Burnt fish (sardine)","369"},
            {"Coins (Shilo Village)","-2"},
            {"Diary (Witch's House)","2408"},
            {"Dragonstone (Mage Training Arena)","6903"},
            {"Holy Grail (item)","Holy Grail"},
            {"Hourglass (Recruitment Drive)","5610"},
            {"Key (Waterfall Dungeon)","298"},
            {"Lamp (Spirits of the Elid)","6796"},
            {"Letter (The Golem)","4615"},
            {"Lever (Temple of Ikov)","83"},
            {"Poison (item)","24"},
            {"Rat poison","273"},
            {"Rock (elemental)","1480"},
            {"Rope (Olaf's Quest)","11046"},
            {"Skull (item)","964"},
            {"Stick (item)","4179"},
            {"Superantipoison","Superantipoison(1)"},
            {"Swamp toad (item)","Swamp toad"},
            {"Tiles (Rogues' Den)","5569,5570,5571"},
        };

        static async Task Main(string[] args)
        {
            Wrapper wrapper = new Wrapper();
            wrapper.Items = new Dictionary<string, List<Item>>();
            if (!File.Exists(@"osrs-items-cleaned.json"))
            {
                Regex locationMatch = new Regex(@"((?<coord>[0-9]+,[0-9]+(,qty:[0-9]+)?))+(\||})");
                using HttpClient client = new HttpClient();
                string lines = File.ReadAllText(@"osrs-urls.txt");
                foreach (string line in lines.Split("\n"))
                {
                    var linesplit = line.Split("\"");
                    string url = linesplit[1];
                    string name = linesplit[3];
                    if (knownConversion.ContainsKey(name))
                    {
                        name = knownConversion[name];
                    }

                    wrapper.Items.Add(name, new List<Item>());
                    HttpRequestMessage request = new HttpRequestMessage(HttpMethod.Get, url);
                    HttpResponseMessage response = await client.SendAsync(request);
                    string content = await response.Content.ReadAsStringAsync();
                    Console.WriteLine($"Scraping `{name}`");
                    foreach (string contentLine in content.Split("\n"))
                    {
                        if (!(contentLine.Contains("}") || contentLine.Contains("{")))
                        {
                            continue;
                        }

                        var matches = locationMatch.Matches(contentLine);
                        int count = matches.Count;
                        foreach (Match match in matches)
                        {
                            string coord = match.Groups["coord"].Value;
                            Item item = new Item();
                            string[] parts = coord.Split(",");
                            item.lat = int.Parse(parts[0]);
                            item.lon = int.Parse(parts[1]);
                            item.qty = parts.Length == 2 ? 1 : int.Parse(parts[2].Split("qty:")[1]);
                            wrapper.Items[name].Add(item);
                        }
                    }
                }

                File.WriteAllText(@"osrs-items-cleaned.json", JsonSerializer.Serialize(wrapper));
            }
            else
            {
                wrapper = JsonSerializer.Deserialize(File.ReadAllText(@"osrs-items-cleaned.json"), typeof(Wrapper)) as Wrapper;
            }
            string lookupFile = File.ReadAllText(@"items-search.json");
            Dictionary<int, Lookup> lookupDict = JsonSerializer.Deserialize(lookupFile, typeof(Dictionary<int, Lookup>)) as Dictionary<int, Lookup>;

            IdWrapper idWrapper = new IdWrapper();
            idWrapper.Items = new Dictionary<int, List<Item>>();

            foreach (string name in wrapper.Items.Keys)
            {
                foreach (string subname in name.Split(","))
                {
                    int id;
                    Console.WriteLine($"Converting `{subname}`");
                    if (int.TryParse(subname, out id))
                    {
                        idWrapper.Items.Add(id, wrapper.Items[name]);
                    }
                    else
                    {
                        idWrapper.Items.Add(lookupDict.Values.First(val => val.name == subname).id, wrapper.Items[name]);
                    }
                }
            }

            File.WriteAllText(@"osrs-items.json", JsonSerializer.Serialize(idWrapper));
        }
    }

    class Wrapper
    {
        public Dictionary<string, List<Item>> Items { get; set; }
    }

    class IdWrapper
    {
        public Dictionary<int, List<Item>> Items { get; set; }
    }

    class Item
    {
        public int lat { get; set; }

        public int lon { get; set; }

        public int qty { get; set; }
    }

    class Lookup
    {
        public int id { get; set; }

        public string name { get; set; }

        public string type { get; set; }

        public bool duplicate { get; set; }
    }
}
